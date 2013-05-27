/*
 * android voice recorder for phone call.
 * copyright 2012, LewaTek Ltd.
 */

#define LOG_TAG "callrec"
//#define LOG_NDEBUG 0

#include "jni.h"
#include "nativehelper/JNIHelp.h"
#include <utils/Log.h>
#include <utils/misc.h>
#include <android_runtime/AndroidRuntime.h>
#include <cutils/properties.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <math.h>
#include <pthread.h>
#include <semaphore.h>
#include <fcntl.h>
#include <limits.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/cdefs.h>
#include <sys/ioctl.h>
#include <sys/time.h>
#include <string.h>
#include <errno.h>
#include <sys/resource.h>

#define array_size(x) (sizeof(x)/sizeof(x[0]))
#include "lame.h"

/* From:  arch/arm/mach-msm/qdsp5_comp/vocpcm.c */
#define VOCPCM_IOCTL_MAGIC 'v'
#define VOCPCM_REGISTER_CLIENT          _IOW(VOCPCM_IOCTL_MAGIC, 0, unsigned)
#define VOCPCM_UNREGISTER_CLIENT        _IOW(VOCPCM_IOCTL_MAGIC, 1, unsigned)

#define FRAME_NUM       (12)
#define FRAME_SIZE      (160)
#define BUFFER_NUM      (2)
#define BUFFER_SIZE     (FRAME_NUM * FRAME_SIZE)

#define READ_SIZE	(BUFFER_SIZE*2)		/* buffers of uint16_t's */
#define RSZ		(16*1024)

// CHSZ must be multiply of READ_SIZE
#define CHSZ (4*READ_SIZE) //(512*1024)
#define OCHSZ (5*(CHSZ/8)+7200)
#define ENCODE_BUFFER_NUM (4)
#define MAX_ERR_COUNT 3
#define MAX_MSG 512

#define RE_SUCCESS 0
#define RE_ERROR -1
#define RE_DISK_FULL -2
#define RE_NO_DISK -3

static JNIEnv * g_env = NULL;
static JavaVM * g_vm = NULL;

static void call_java_callback(const char* func, int msg_code);

#if LOG_NDEBUG
#define MYLOGV(fmt, ...) ((void)0)
#else
inline void MYLOGV(const char * fmt, ...) 
{
    va_list ap;
    char msg[MAX_MSG];
    va_start(ap, fmt);
    vsnprintf(msg, sizeof(msg), fmt, ap);
    va_end(ap);

    timeval t;
    gettimeofday(&t, 0);
    LOGV("[%ld.%06d][%d] %s", t.tv_sec, (int)t.tv_usec, gettid(), msg);
}
#endif

inline void MYLOGI(const char * fmt, ...) 
{
    va_list ap;
    char msg[MAX_MSG];
    va_start(ap, fmt);
    vsnprintf(msg, sizeof(msg), fmt, ap);
    va_end(ap);

    timeval t;
    gettimeofday(&t, 0);
    LOGI("[%ld.%06d][%d] %s", t.tv_sec, (int)t.tv_usec, gettid(), msg);
}

// set core dump limit block size
// blocks: -1 for unlimit, 0 for disable
void set_coredump(int blocks)
{
    struct rlimit coredump;
    memset(&coredump, 0, sizeof(struct rlimit));
    coredump.rlim_cur = (blocks==-1?RLIM_INFINITY:blocks);
    coredump.rlim_max = (blocks==-1?RLIM_INFINITY:blocks);
    setrlimit(RLIMIT_CORE, &coredump);
}

struct rec_enc_queue
{
    struct buffer_t
    {
        volatile int len; //actual data size
        int16_t * buffer;
    };

    buffer_t buffer_rx[ENCODE_BUFFER_NUM];
    buffer_t buffer_tx[ENCODE_BUFFER_NUM];

    int _head;
    int _tail;
    int _length;
   
    sem_t _head_sem; // control how many buffer can be read
    sem_t _tail_sem; // control how many buffer can be write

    rec_enc_queue()
    {
        bzero(buffer_rx, sizeof(buffer_rx));
        bzero(buffer_tx, sizeof(buffer_tx));   
        _head = _tail = 0;
        _length = array_size(buffer_rx);

        sem_init(&_head_sem, 0, 0);
        sem_init(&_tail_sem, 0, _length);
    }

    ~rec_enc_queue()
    {
        sem_destroy(&_head_sem);
        sem_destroy(&_tail_sem);
    }

    bool init()
    {
        uint32_t i=0;
        for (i=0; i<array_size(buffer_rx); i++) {
            if ( (int)(&buffer_rx[i].len) % 4 != 0) LOGW("bufferrx %d len not aligned at 32 bits", i);
            if ( (int)(&buffer_tx[i].len) % 4 != 0) LOGW("buffertx %d len not aligned at 32 bits", i);
            if((buffer_rx[i].buffer = (int16_t*)malloc(CHSZ)) == NULL) break;
            if((buffer_tx[i].buffer = (int16_t*)malloc(CHSZ)) == NULL) break;
        }

        _head = _tail = 0;
        _length = array_size(buffer_rx);
        return (i >= array_size(buffer_rx));
    }
    
    void release()
    {
        sem_post(&_tail_sem);
        sem_post(&_head_sem);
    }

    void destroy()
    {
        for (uint32_t i=0; i<array_size(buffer_rx); i++) {
            if(buffer_rx[i].buffer != NULL) free(buffer_rx[i].buffer);
            if(buffer_tx[i].buffer != NULL) free(buffer_tx[i].buffer);
            buffer_rx[i].buffer = NULL;
            buffer_tx[i].buffer = NULL;
        }

    }    

    // called before try to access head, usually a consumer do it
    int read_begin()
    {
        sem_wait(&_head_sem);
        return _head; 
    }

    // a read operation at head has completed
    void read_end()
    {
        sem_post(&_tail_sem);
        _head = (_head+1) % _length;
    }

    // called before try to access tail, usually a producer do it
    int write_begin()
    {
        sem_wait(&_tail_sem);
        return _tail;
    }

    void write_end()
    {
        sem_post(&_head_sem);
        _tail = (_tail+1) % _length;
    }

    int get_read_count()
    {
        int count = 0;
        if (sem_getvalue(&_head_sem, &count) == 0) {
            return count;
        } else {
            return 0;
        }
    }

    int get_write_count()
    {
        int count = 0;
        if (sem_getvalue(&_tail_sem, &count) == 0) {
            return count;
        } else {
            return 0;
        }
    }
};

struct encode_context
{
    bool volatile alive;
    int volatile record_started;
    int boost_up;
    int boost_dn;
    int volatile err_count;
    char cur_file[256];
    pthread_t rec_thread, enc_thread;
    rec_enc_queue buff_queue;
    int volatile fd_rx, fd_tx;
    int last_codec;
    int volatile last_error;
    encode_context()
    {
        alive = false;
        record_started = 0;
        boost_up = boost_dn = 0;
        fd_rx = fd_tx = -1;
        last_codec = 1;
        err_count = 0;
        last_error = 0;
    }

};

void* record(void *);
void* encode(void *);
void* encode_incoming(void *);
void* closeup(void* init);
double mytimersub(timeval begin, timeval end);

encode_context * g_lastctx = NULL;
static bool g_coredumpset = false;
/* void stop_record(JNIEnv* env, jobject obj, jint codec); */
static void android_phone_vocrec_stopRecord(
    JNIEnv* env, jobject obj);

static void check_buffer_safe(encode_context * ctx);

static jint android_phone_vocrec_startRecord(
    JNIEnv* env, jobject obj, jstring jfile, jint encoding_format, jint jbu, jint jbd) 
{ 
    pthread_attr_t attr;
    const char *file = 0;

    MYLOGI("start_record");

    if (!g_coredumpset) {
        set_coredump(-1);

        g_coredumpset = true;
    }

    android_phone_vocrec_stopRecord(env, obj);

    struct encode_context* enc_ctx = new encode_context();
    g_lastctx = enc_ctx;
    if ((int)(&enc_ctx->alive) % 4 != 0) LOGW("alive unaligned at 32 bits");
    check_buffer_safe(enc_ctx);
    enc_ctx->last_codec = encoding_format;

    if (!jfile) return 1;

    file = env->GetStringUTFChars(jfile, NULL);
    if (!file || !*file) {
        env->ReleaseStringUTFChars(jfile, file);
        LOGE("bad string from jni");
        return 1;
    }
    
    strncpy(enc_ctx->cur_file, file, sizeof(enc_ctx->cur_file)-1);
    enc_ctx->cur_file[sizeof(enc_ctx->cur_file)-1] = '\0';
    env->ReleaseStringUTFChars(jfile, file);
    enc_ctx->boost_up = jbu;
    enc_ctx->boost_dn = jbd;

    if (enc_ctx->buff_queue.init()) {
        enc_ctx->alive = true;
        pthread_attr_init(&attr);
        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

        pthread_create(&enc_ctx->rec_thread, &attr, record, (void*)enc_ctx);
        pthread_create(&enc_ctx->enc_thread, &attr, encode, (void*)enc_ctx);

        pthread_attr_destroy(&attr);
    } else {
        LOGE("init buffer queue failed");
    }


    return 0;
}

struct timeval stop_start, stop_end;
double mytimersub(timeval begin, timeval end)
{
    double elapsed = end.tv_sec - begin.tv_sec + (double)(end.tv_usec - begin.tv_usec)/1000000;
    return elapsed;
}

void* closeup(void* init)
{
    encode_context* enc_ctx = (encode_context*)init;
    // ensure all wait on sem to wakeup
    enc_ctx->buff_queue.release();
    pthread_join(enc_ctx->enc_thread, 0);
    enc_ctx->buff_queue.destroy();
    delete enc_ctx;
    gettimeofday(&stop_end, 0);
  
    MYLOGI("use %f seconds to stop record", mytimersub(stop_start, stop_end));
    return 0;
}

static void android_phone_vocrec_stopRecord(
    JNIEnv* env, jobject obj) 
{

    if (g_lastctx != NULL) {
        gettimeofday(&stop_start, 0);
        if (g_lastctx->alive) {
            MYLOGI("stop_record");
            g_lastctx->alive = false; 
            // unregister vocpcm client here to make record thread quit fast
            ioctl(g_lastctx->fd_tx, VOCPCM_UNREGISTER_CLIENT, 0);
            ioctl(g_lastctx->fd_rx, VOCPCM_UNREGISTER_CLIENT, 0);

            pthread_t close_thread;
            pthread_create(&close_thread, NULL, closeup, (void*)g_lastctx);
        }
        g_lastctx = NULL;
    }
}

// open device use open system call, if device is busy, auto retry
// retrycount: how many times to rety, 0.1 second between two opens 
int open_retry(const char * device, int flags, int retrycount)
{
    int fd = -1;
    int count = 0;
    do {
        fd = open(device, flags);
        if (fd < 0) {
            if (errno == EBUSY && count < retrycount) {
                usleep(100000); // 0.1 second
                count++;
            } else {
                break;
            }
        } else {
            break;
        }
    } while (true);

    return fd;
}

void * record(void * init)
{
    struct encode_context *enc_ctx = (encode_context*)init;
    MYLOGI("record thread start...");

    enc_ctx->fd_tx = open_retry("/dev/voc_tx_record", O_RDWR, 50);
    if (enc_ctx->fd_tx < 0) {
        LOGE("cannot open %s driver; %d(%s)", "uplink", errno, strerror(errno));
    }
    else if ((enc_ctx->fd_rx = open_retry("/dev/voc_rx_record", O_RDWR, 50)) < 0) {
        LOGE("cannot open %s driver; %d(%s)", "downlink", errno, strerror(errno));
    }
    else {
        /* positive return values are ok for this ioctl */
        if (ioctl(enc_ctx->fd_tx, VOCPCM_REGISTER_CLIENT, 0) < 0) {
            enc_ctx->last_error = RE_ERROR;
            LOGE("cannot register tx rpc client");
        }
        else if (ioctl(enc_ctx->fd_rx, VOCPCM_REGISTER_CLIENT, 0) < 0) {
            enc_ctx->last_error = RE_ERROR;
            LOGE("cannot register rx rpc client");
        } else { // register rx and tx rpc client successfully

            enc_ctx->record_started = 1;
            enc_ctx->err_count = 0;
            int ptr = 0; // current position in buffer
            while (enc_ctx->alive) {
                ptr = 0;
                int tail = enc_ctx->buff_queue.write_begin();
                MYLOGV("time:%ld, write at buffer %d", time(NULL), tail);
                if(!enc_ctx->alive) {
                    enc_ctx->buff_queue.write_end();
                    break;
                }

                int16_t * buff_tx = enc_ctx->buff_queue.buffer_tx[tail].buffer;
                int16_t * buff_rx = enc_ctx->buff_queue.buffer_rx[tail].buffer;
                do {
                    int rx = read(enc_ctx->fd_rx, (unsigned char*)buff_rx+ptr, READ_SIZE);
                    if (rx < 0) {
                        enc_ctx->err_count++;
                        MYLOGV("read error %s in read_live thread", strerror(errno));
                        if (enc_ctx->err_count == MAX_ERR_COUNT) {
                            LOGE("max read err count in read_live thread reached");
                            break;
                        }
                    } else if (rx > READ_SIZE) {
                        LOGE("read return wrong %d", rx);
                    }
                    
                    if(!enc_ctx->alive) break;
                    int tx = read(enc_ctx->fd_tx, (unsigned char*)buff_tx+ptr, READ_SIZE);

                    if (tx < 0) {
                        enc_ctx->err_count++;
                        MYLOGV("read error %s in read_live thread", strerror(errno));
                        if (enc_ctx->err_count == MAX_ERR_COUNT) {
                            LOGE("max read err count in read_live thread reached");
                            break;
                        }
                    } else if (tx > READ_SIZE) {
                        LOGE("read return wrong %d", tx);
                    }

                    if (rx > 0 && tx > 0) {
                        if(rx > tx) rx = tx;
                        ptr += rx;
                    }
                }
                while(ptr < CHSZ && enc_ctx->alive);

                if (ptr > CHSZ) { 
                    LOGW("in record %d bytes, buffer overrun...", ptr);
                    ptr = 0; // avoid buffer overflow
                }

                enc_ctx->buff_queue.buffer_tx[tail].len = ptr;
                enc_ctx->buff_queue.buffer_rx[tail].len = ptr;
                enc_ctx->buff_queue.write_end();

                if (enc_ctx->err_count == MAX_ERR_COUNT) {
                    enc_ctx->last_error = RE_ERROR;
                    break;
                }
            }
        }

        ioctl(enc_ctx->fd_tx, VOCPCM_UNREGISTER_CLIENT, 0);
        ioctl(enc_ctx->fd_rx, VOCPCM_UNREGISTER_CLIENT, 0);
    }

    // make sure encode thread quit
    enc_ctx->alive = false;
    enc_ctx->buff_queue.write_end();

    if(enc_ctx->fd_tx >= 0) close(enc_ctx->fd_tx);
    if(enc_ctx->fd_rx >= 0) close(enc_ctx->fd_rx);
    enc_ctx->fd_tx = enc_ctx->fd_rx = -1;
    
    MYLOGI("record thread end...");
    return 0;
}

static struct {
    uint32_t riff_id;
    uint32_t riff_sz;
    uint32_t riff_fmt;
    uint32_t fmt_id;
    uint32_t fmt_sz;
    uint16_t audio_format;
    uint16_t num_channels;
    uint32_t sample_rate;
    uint32_t byte_rate;       /* sample_rate * num_channels * bps / 8 */
    uint16_t block_align;     /* num_channels * bps / 8 */
    uint16_t bits_per_sample;
    uint32_t data_id;
    uint32_t data_sz;
} wavhdr = { 0x46464952, 0, 0x45564157, 0x20746d66, 16, 1, 2, 8000, 32000, 4, 16, 0x61746164, 0};

void lame_error_handler(const char *format, va_list ap) 
{
    MYLOGV(format, ap);
}

static void recording_complete();
static void encoding_complete();

static inline int16_t boost_word(int16_t j, int boost) 
{
    int32_t i = j << boost;
    if (i > ((1<<15)-1)) 
      i = ((1<<15)-1);
    else if (i < -(1<<15)) 
      i = -(1<<15);
    return i;
}

static void boost_buff(int16_t *buff, size_t bufsz, int boost) 
{
    size_t i;
    for (i = 0; i < bufsz; i++) 
      buff[i] = boost_word(buff[i], boost);
}


// callback for processing audio data
// return true to continue, false to abort
bool on_data_mp3(void * buff_tx, void* buff_rx, void * buff_out, 
        int fd_out, int count, int boost_up, int boost_dn, void * handle)
{
    lame_global_flags * gfp = (lame_global_flags*)handle;


    if (boost_up) boost_buff((int16_t*)buff_tx, count/2, boost_up);
    if (boost_dn) boost_buff((int16_t*)buff_rx, count/2, boost_dn);
    int ret = lame_encode_buffer(gfp, (int16_t*)buff_tx, (int16_t*)buff_rx, count/2, (unsigned char *)buff_out, OCHSZ);
    
    if (ret >= 0) {
        unsigned char *c1 = (unsigned char *) buff_out;
        count = ret;
        int j = 0;
        for (j = 0; j < count; j += RSZ, c1 += ret) {
            ret = (j + RSZ > count) ? count - j : RSZ;
            if(write(fd_out, c1, ret) < 0) break;
        }

        if (j >= count) {
            return true;
        } else {
            return false;
        }
    } else {
        return false;
    }
}

bool on_data_wave(void * buff_tx, void* buff_rx, void * buff_out, 
        int fd_out, int count, int boost_up, int boost_dn, void * handle)
{
    int j = 0;
    int16_t * b0 = (int16_t*)buff_out;
    int16_t * b1 = (int16_t*)buff_tx;
    int16_t * b2 = (int16_t*)buff_rx;

    if (boost_up && boost_dn) {
        for (j = 0; j < count/2; j++) {
            b0[2*j] = boost_word(b1[j], boost_up);
            b0[2*j+1] = boost_word(b2[j], boost_dn);
        }
    } else if (boost_up) {
        for (j = 0; j < count/2; j++) {
            b0[2*j] = boost_word(b1[j], boost_up);
            b0[2*j+1] = b2[j];
        }
    } else if (boost_dn) {
        for (j = 0; j < count/2; j++) {
            b0[2*j] = b1[j];
            b0[2*j+1] = boost_word(b2[j], boost_dn);
        }
    } else {
        for (j = 0; j < count/2; j++) { 
            b0[2*j] = b1[j];
            b0[2*j+1] = b2[j];
        }
    }

    write(fd_out, b0, count*2);
    return true;
}

void * on_start_mp3()
{
    lame_global_flags *gfp;
 
    gfp = lame_init();
    lame_set_errorf(gfp, lame_error_handler);
    lame_set_debugf(gfp, lame_error_handler);
    lame_set_msgf(gfp, lame_error_handler);
    lame_set_num_channels(gfp, 2);
    lame_set_in_samplerate(gfp, 8000);
#if 0
    lame_set_brate(gfp, 64); /* compress 1:4 */
    lame_set_mode(gfp, 0);	/* mode = stereo */
    lame_set_quality(gfp, 2);   /* 2=high  5 = medium  7=low */
#else
    lame_set_quality(gfp, 5);   /* 2=high  5 = medium  7=low */
    lame_set_mode(gfp, (MPEG_mode)3);	/* mode = mono */
    if (lame_get_VBR(gfp) == vbr_off) lame_set_VBR(gfp, vbr_default);
    lame_set_VBR_quality(gfp, 7.0);
    lame_set_findReplayGain(gfp, 0);
    lame_set_bWriteVbrTag(gfp, 1);
    lame_set_out_samplerate(gfp, 11025);
    /* lame_set_num_samples(gfp, i/2); */
#endif
    if (lame_init_params(gfp) < 0) {
        lame_close(gfp);
        LOGE("failed to init lame");
        return 0;
    } else {
        return gfp;
    }

}

void * on_start_wave(int fd_out)
{
    write(fd_out, &wavhdr, sizeof(wavhdr));
    return &wavhdr;
}

void on_end_mp3(void * data, int fd_out, void* handle)
{
    lame_global_flags * gfp = (lame_global_flags*)handle;
    int i = (uint32_t) lame_encode_flush(gfp, (unsigned char *)data, OCHSZ);
    if (i) write(fd_out, data, i);
    i = lame_get_lametag_frame(gfp, (unsigned char *)data, OCHSZ);
    if (i>0) write(fd_out, data, i);

    lame_close((lame_global_flags*)handle);
}

void on_end_wave(void * data, int fd_out, void* handle)
{
    off_t leng = lseek(fd_out, 0, SEEK_END);
    leng -= sizeof(wavhdr);
    wavhdr.num_channels = 2;
    wavhdr.byte_rate = 32000;
    wavhdr.block_align = 4;
    wavhdr.data_sz = leng;
    wavhdr.riff_sz = leng+ 36;
    lseek(fd_out, 0, SEEK_SET);

    write(fd_out, &wavhdr, sizeof(wavhdr));
}

static void check_buffer_safe(encode_context * ctx)
{
   uint32_t i=0;
   for (i=0; i<array_size(ctx->buff_queue.buffer_rx); i++) {
       int len = ctx->buff_queue.buffer_rx[i].len;
       if (len > CHSZ) LOGW("buffer %d, %d, overrun!!!", i, len);
       len = ctx->buff_queue.buffer_tx[i].len; 
       if (len > CHSZ) LOGW("buffer %d, %d, overrun!!!", i, len);
   }
 
}

bool encode_once(encode_context* ctx, void * handle, int fd_out, int16_t* buff_out, int * data_read, bool mp3)
{
    int head = ctx->buff_queue.read_begin();
    MYLOGV("time: %ld, read at buffer %d", time(NULL), head);

    int16_t* buff_tx = ctx->buff_queue.buffer_tx[head].buffer;
    int16_t* buff_rx = ctx->buff_queue.buffer_rx[head].buffer;
    int datalen = ctx->buff_queue.buffer_tx[head].len;
    
    if (datalen > CHSZ) {
        LOGW("in encode %d bytes, buffer overrun...", datalen);
        datalen = 0;
    }

    bool success = false;
    if (datalen > 0) {                    
        MYLOGV("recorded %d bytes data, encode it", datalen);

        bool (*on_data_func)(void*, void*, void*, int, int, int, int, void*);
        on_data_func = mp3?on_data_mp3:on_data_wave;
        if (on_data_func(buff_tx, buff_rx, buff_out, fd_out, 
                    datalen, ctx->boost_up, ctx->boost_dn, handle)) {
            if(data_read) *data_read = datalen;
            success = true;
        } else if(errno == ENOSPC) {
            ctx->last_error = RE_DISK_FULL;
        } else {
            ctx->last_error = RE_ERROR;
        }

        //check_buffer_safe(ctx);
        
    } else {
        success = true;
        MYLOGV("no audio data in buffer, ignore...");
    }
    
    ctx->buff_queue.buffer_tx[head].len = 0;
    ctx->buff_queue.buffer_rx[head].len = 0;
    ctx->buff_queue.read_end();
    return success;
}

// doing recording and encoding at same time
void *encode(void *init)
{
    struct encode_context *enc_ctx = (encode_context*)init;
    char file[256]; 
    int fd_out;// isup = (int) init;
    int err_count;
    off_t rx_count = 0, tx_count = 0, off_out = 0;
    bool mp3 = (int)enc_ctx->last_codec & 1;
    int16_t *buff_rx=NULL, *buff_tx=NULL, *buff_out=NULL;
    struct timeval start, stop, tm;
    void * handle = NULL;
    bool success = false;
    bool encode_exit = false;
    bool msg_posted = false;
    
    MYLOGI("encode as %s file", mp3?"mp3":"wave");

    gettimeofday(&start, 0);

    strncpy(file, enc_ctx->cur_file, sizeof(file));
    file[sizeof(file)-1] = 0;

    fd_out = open(file, O_CREAT|O_WRONLY|O_TRUNC);
    if (fd_out >=0) {
        if (mp3) {
            handle = on_start_mp3();
        } else {
            handle = on_start_wave(fd_out);
        }

        if (handle == NULL) {
            LOGE("encode init failed");
        } else {
            int16_t * buff_out = NULL;
                
            if (mp3) {
                buff_out = (int16_t*)malloc(OCHSZ);
            } else {
                buff_out = (int16_t*)malloc(CHSZ*2);
            }

            if (buff_out) {
                while (enc_ctx->alive) {
                    int data_read = 0;
                    if (encode_once(enc_ctx, handle, fd_out, buff_out, &data_read, mp3)) {
                        rx_count += data_read;
                        tx_count += data_read;
                        success = true;
                    } else {
                        success = false;
                        break;
                    }
                }
                
                if (success == false) {
                    enc_ctx->alive = false;
                    enc_ctx->buff_queue.read_end(); // notify encode to quit
                    LOGE("encode failed, wait record thread to quit...");
                } else {
                    call_java_callback("onMessage", RE_SUCCESS);
                    msg_posted = true;
                }   
                pthread_join(enc_ctx->rec_thread, 0);
                LOGV("join: record thread exit");
                encode_exit = true;

                if (success == true) {
                    // encode remained data
                    while (enc_ctx->buff_queue.get_read_count() > 0) {
                        int data_read = 0;
                        MYLOGV("encode a remained buffer");
                        if (encode_once(enc_ctx, handle, fd_out, buff_out, &data_read, mp3)) {
                            rx_count += data_read;
                            tx_count += data_read;
                        } else { 
                            break; // ignore error here
                        }
                    }

                    MYLOGV("flush audio data");
                    if (mp3) {
                        on_end_mp3(buff_out, fd_out, handle);
                    } else {
                        on_end_wave(buff_out, fd_out, NULL);
                    }
                }
                free(buff_out);
            }
        }

        if (success == true) {
            off_out = lseek(fd_out, 0, SEEK_END);
            gettimeofday(&stop, 0);
            timersub(&stop, &start, &tm);
            MYLOGI("encoding complete: %ld -> %ld in %ld sec", rx_count+tx_count, off_out, tm.tv_sec);
            enc_ctx->last_error = RE_SUCCESS;
        }
        
        enc_ctx->record_started = 0;

        close(fd_out);
    } else {
        if (errno == ENOSPC) {
            enc_ctx->last_error = RE_DISK_FULL;
        } else {
            enc_ctx->last_error = RE_ERROR;
        }
        LOGE("cannot open output file \'%s\'", enc_ctx->cur_file);
    }

    enc_ctx->alive = false;
    enc_ctx->buff_queue.read_end();
    if (!encode_exit) {
        pthread_join(enc_ctx->rec_thread, 0);
    }

    if(!msg_posted) call_java_callback("onMessage", enc_ctx->last_error);
    
    MYLOGI("encode thread end...");

    return 0;
}

/******   Answer the incoming call  ******/
static void *say_them(void *);
static void *dummy_write(void *p);

/***** Java callback to hangup. Not reached on Hero
(the phone rather hangs itself or reboots) *****/
static void  call_java2hangup();
static void  aa_rec_started();

static int aa_file = -1;
static int aa_fd = -1;

//void answer_call(JNIEnv* env, jobject obj, jstring jfile, jstring ofile, jint jbd) {
//    pthread_t pt;
//    const char *file = 0;
//
//    MYLOGV("in answer_call");
//    record_started = 0;
//    aa_fd = -1;
//
//    file = env->GetStringUTFChars(jfile, NULL);
//    if (!file || !*file) {
//        env->ReleaseStringUTFChars(jfile, file);
//        LOGE("bad string from jni");
//        return;
//    }
//    aa_file = open(file, O_RDONLY);
//    if (aa_file < 0) {
//        env->ReleaseStringUTFChars(jfile, file);
//        LOGE("cannot open auto answer sound file %s", file);
//        return;
//    }
//    if (strlen(file)> 4 && strcmp(file+strlen(file)-4, ".wav")==0)
//        lseek(aa_file, sizeof(wavhdr), SEEK_SET); /* don't bother checking wav header */
//    env->ReleaseStringUTFChars(jfile, file);
//
//    file = env->GetStringUTFChars(ofile, NULL);
//    if (file && *file) strcpy(cur_file, file);
//    else *cur_file = 0;
//
//    env->ReleaseStringUTFChars(ofile, file);
//
//    if (*cur_file) MYLOGV("will record to %s after answering", cur_file);
//    else MYLOGV("will hang up after answering");
//
//    boost_dn = jbd;
//
//    pthread_create(&pt, 0, say_them, 0);
//}
//
//
//static void *say_them(void *p) {
//    char *buff;
//
//    MYLOGV("in say_them() thread");
//
//    aa_fd = open("/dev/voc_tx_playback", O_RDWR);
//    if (aa_fd < 0)  {
//        aa_fd = open("/dev/vocpcm3", O_RDWR);
//        if (aa_fd < 0) {
//            close(aa_file);
//            aa_file = -1;
//            LOGE("cannot open playback driver");
//            return 0;
//        }
//    }
//    if (ioctl(aa_fd, VOCPCM_REGISTER_CLIENT, 0) < 0) {
//        close(aa_fd);
//        close(aa_file);
//        aa_file = -1;
//        LOGE("cannot register rpc client for playback");
//        return 0;
//    }
//
//    buff = (char *) malloc(BUFFER_SIZE);
//    memset(buff, 0, BUFFER_SIZE);
//
//    write(aa_fd, buff, BUFFER_SIZE);
//    write(aa_fd, buff, BUFFER_SIZE); At global scope:

//
//    MYLOGV("answering...");
//    alive = 1;
//
//    while (alive) {
//        int i, m;
//        i  = read(aa_file, buff, BUFFER_SIZE);
//        if (i <= 0) break;
//        m = write(aa_fd, buff, i);
//        if (m < 0)  {
//            MYLOGV("playback: i'm probably closed from outside");
//            pthread_mutex_lock(&mutty);
//            alive = 0;
//            pthread_mutex_unlock(&mutty); 
//            break;
//        }
//    }
//
//    close(aa_file);
//    aa_file = -1;
//#if 0
//    close(aa_fd);
//    aa_fd = -1;
//#endif
//    free(buff);
//    if (!alive || *cur_file == 0) {
//        ioctl(aa_fd, VOCPCM_UNREGISTER_CLIENT, 0);
//        close(aa_fd);
//        aa_fd = -1;
//        MYLOGV("now hanging up in java");
//        call_java2hangup();
//    } else {
//        pthread_attr_t attr;
//        pthread_attr_init(&attr);
//        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE); 
//        /* records incoming sound data */
//        pthread_create(&rec1, &attr, record, (void *) 0);
//#if 1
//        pthread_create(&rec2, &attr, dummy_write, (void *) 0);
//#endif
//        aa_rec_started();
//    }
//    return 0;
//}
//
//static void *dummy_write(void *p) {
//    char *buff;
//    buff = (char *) malloc(BUFFER_SIZE);
//    memset(buff, 0, BUFFER_SIZE);
//    MYLOGV("in dummy_write thread");
//    if (aa_fd < 0) return 0;
//    while (alive) {
//        if (write(aa_fd, buff, BUFFER_SIZE) < 0) break;
//    }
//    ioctl(aa_fd, VOCPCM_UNREGISTER_CLIENT, 0);
//    close(aa_fd); 
//    aa_fd = -1;
//    free(buff);
//    MYLOGV("exiting dummy_write thread");
//    return 0;
//}
//

static void call_java_callback(const char *function, int msg_code) 
{ 
    jclass cls;
    jmethodID mid;
    JNIEnv * env = NULL;     
    if (g_vm->AttachCurrentThread(&env, NULL) != JNI_OK) {
      LOGE("AttachCurrentThread failed");
    } else {    
        cls = env->FindClass("android/phone/CallRecorder");
        if (!cls) {
          LOGE("GetObjectClass failed");
        } else {
            mid = env->GetStaticMethodID(cls, function, "(I)V");
            if (mid == NULL) {
              LOGE("cannot find java callback to call");
            } else {
                env->CallStaticVoidMethod(cls, mid, msg_code);
            }
        }
        g_vm->DetachCurrentThread();
    }
     
}

static void call_java2hangup()
{
    call_java_callback("onCallAnswered", 0);
    MYLOGV("java onCallAnswered notified");
}

static void recording_complete() 
{
    call_java_callback("onRecordingComplete", 0);
    MYLOGV("java onRecordingComplete notified");
}

static void encoding_complete() 
{
    call_java_callback("onEncodingComplete", 0);
    MYLOGV("java onEncodingComplete notified");
}

static void aa_rec_started() 
{
    call_java_callback("onAutoanswerRecordingStarted", 0);
    MYLOGV("java onAutoanswerRecordingStarted notified");
}

/*
 * JNI registration.
 */
static JNINativeMethod gMethods[] = {
    /* name, signature, funcPtr */
    { "stopRecord", "()V",
        (void*)android_phone_vocrec_stopRecord },
    { "startRecord", "(Ljava/lang/String;III)I",
      (void*)android_phone_vocrec_startRecord },
};

int register_android_phone_vocrec(JNIEnv* env)
{
    g_env = env;
    g_env->GetJavaVM(&g_vm);
    return jniRegisterNativeMethods(env, "android/phone/CallRecorder", gMethods, NELEM(gMethods));
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;

    jint result = -1;
    
    LOGV("libcallrec loading...");
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) == JNI_OK) { 
        register_android_phone_vocrec(env);
        result = JNI_VERSION_1_4;
    } else {
        LOGE("getEnv failed in JNI_OnLoad");
    }

    return result;
} 
