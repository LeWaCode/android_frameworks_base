package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.R;

import android.content.Context;
import android.widget.DragableListView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.view.Gravity;
import android.view.KeyEvent;
import android.provider.Settings;
import android.util.AttributeSet;
import android.graphics.PixelFormat;
import android.util.Log;

import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

public class SwitchWidgetOptionsView extends LinearLayout {
    private static final String TAG = "SwitchWidgetOptionsView";
    private static final boolean DBG = true;

    private static SwitchWidgetOptionsView sInstance;
    private static WindowManager.LayoutParams sLayoutParams;

    private DragableListView mButtonList;
    private ButtonAdapter mButtonAdapter;
    private int mPrevHeight = -1;

    private ArrayList<ButtonInfo> mButtons;
    private ArrayList<String> mButtonStrings;

    private String mCurrentButtons;
    private String mCurrentTinyButtons;

    private DragableListView.DropListener mDropListener = new DragableListView.DropListener() {
            public void drop(int from, int to) {
                if(from < mButtons.size()) {
                    ButtonInfo button = mButtons.remove(from);
                    String str = mButtonStrings.remove(from);

                    if (to <= mButtons.size()) {
                        mButtons.add(to, button);
                        mButtonStrings.add(to, str);
                        // tell our adapter/listview to reload
                        mButtonList.invalidateViews();
                    }
                }
            }
        };

    public static void show(Context context) {
        if (null == sInstance) {
            sInstance = (SwitchWidgetOptionsView) View.inflate(
                    context, R.layout.switch_buttons_order, null);

            sLayoutParams = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,    // width
                    ViewGroup.LayoutParams.MATCH_PARENT,    // height
                    WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                        | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING,
                    PixelFormat.RGBX_8888);
            sLayoutParams.gravity = Gravity.TOP | Gravity.FILL;
            sLayoutParams.setTitle("SwitchWidgetOptions");
            sLayoutParams.type = WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL;
            sLayoutParams.windowAnimations = com.android.internal.R.style.Animation_Dialog;
        }

        WindowManagerImpl.getDefault().addView(sInstance, sLayoutParams);
    }

    private void hide() {
        if (null != sInstance) {
            WindowManagerImpl.getDefault().removeView(sInstance);
            sInstance = null;
        }
    }

    public SwitchWidgetOptionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        mButtonAdapter = new ButtonAdapter(LayoutInflater.from(getContext()));
        mButtonList.setAdapter(mButtonAdapter);
        mButtonList.setDropListener(mDropListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        mButtonList.setAdapter(null);
        mButtonList.setDropListener(null);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        switch (event.getKeyCode()) {
        case KeyEvent.KEYCODE_BACK:
            if (!down) {
                hide();
                saveCurrentButtons();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mButtonList = (DragableListView) findViewById(R.id.listview);
        // mButtonList.setWindowType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);
        mButtonList.setWindowType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);

        // mButtonList.setItemsCanFocus(false);
    }

    /** We want to shrink down to 0, and ignore the background. */
    @Override
    public int getSuggestedMinimumHeight() {
        return 0;
    }

    @Override
     protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
         super.onLayout(changed, left, top, right, bottom);
         int height = bottom - top;
         if (height != mPrevHeight) {
             mPrevHeight = height;
             // mService.updateExpandedViewPos(StatusBarService.EXPANDED_LEAVE_ALONE);
         }
     }

    private class ButtonAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public ButtonAdapter(LayoutInflater flater) {
            mInflater = flater;
            reloadButtons();
        }

        // In dual pages mode, all buttons are showed in the order you select,
        // but in single page mode, only selected buttons are showed
        public void reloadButtons() {
            for (ButtonInfo button : BUTTONS.values()) {
                button.setSelected(false);
            }

            // First shows all of the buttons
            mCurrentButtons = getCurrentButtons(false);
            // if (DBG) {
                // Log.d(TAG, "Buttons loaded from settings: " + mCurrentButtons);
            // }
            ArrayList<String> buttons = getButtonListFromString(mCurrentButtons);
            mButtons = new ArrayList<ButtonInfo>();
            mButtonStrings = new ArrayList<String>();
            for (String button : buttons) {
                if (BUTTONS.containsKey(button)) {
                    mButtons.add(BUTTONS.get(button));
                    mButtonStrings.add(button);
                }
            }

            // Set the buttons showed in tiny mode as selected
            mCurrentTinyButtons = getCurrentButtons(true);
            // if (DBG) {
                // Log.d(TAG, "Tiny buttons loaded from settings: " + mCurrentTinyButtons);
            // }
            buttons = getButtonListFromString(mCurrentTinyButtons);
            int index;
            ButtonInfo bi;
            for (String button : buttons) {
                index = mButtonStrings.indexOf(button);
                if (index != -1) {
                    bi = (ButtonInfo) mButtons.get(index);
                    bi.setSelected(true);
                }
            }

            // Switch toggle should always be selected
            /*
             * index = mButtonStrings.indexOf(SwitchButton.BUTTON_SWITCH_WIDGET_STYLE);
             * if (index != -1) {
             *     bi = (ButtonInfo) mButtons.get(index);
             *     if (!bi.isSelected()) {
             *         bi.setSelected(true);
             *     }
             * }
             */
        }

        public int getCount() {
            return mButtons.size();
        }

        public Object getItem(int position) {
            return mButtons.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.switch_buttons_order_item, null);
            }

            ItemViewHolder holder = (ItemViewHolder) convertView.getTag();
            if (holder == null) {
                holder = new ItemViewHolder();

                holder.ICON = (ImageView) convertView.findViewById(R.id.icon);
                holder.NAME = (TextView) convertView.findViewById(R.id.name);
                holder.CHECK = (CheckBox)
                        convertView.findViewById(R.id.selection);

                holder.CHECK.setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        final int pos = (Integer) buttonView.getTag();
                        final ButtonInfo bi = mButtons.get(pos);
                        bi.setSelected(isChecked);
                        ((BaseAdapter) mButtonList.getAdapter()).notifyDataSetChanged();
                        if (DBG) {
                            Log.d(TAG, "setOnCheckedChangeListener - " + bi.getId() + " set to " + isChecked);
                        }
                    }
                });
                convertView.setTag(holder);
            }

            holder.CHECK.setTag(position);

            ButtonInfo button = mButtons.get(position);

            if (DBG) {
                Log.d(TAG, "getView - Position: "
                        + Integer.toString(position) + "; ButtonInfo: " + (button == null ? "NULL" : button.getId()));
            }

            if (null != button) {
                if (DBG) {
                    Log.d(TAG, "Content checked: " + button.isSelected()
                            + "; Display checked: " + holder.CHECK.isChecked());
                }
                holder.CHECK.setChecked(button.isSelected());
                holder.NAME.setText(button.getTitleResId());

                // assume no icon first
                holder.ICON.setVisibility(View.GONE);
                // attempt to load the icon for this button
                holder.ICON.setVisibility(View.VISIBLE);
                holder.ICON.setImageResource(button.getIconResId());
            }

            return convertView;
        }
    }

    private final class ItemViewHolder {
        public ImageView ICON;
        public TextView NAME;
        public CheckBox CHECK;
    }

    public static HashMap<String, ButtonInfo> BUTTONS = new HashMap<String, ButtonInfo>();
    static {
        BUTTONS.put(SwitchButton.BUTTON_AIRPLANE, new ButtonInfo(
                SwitchButton.BUTTON_AIRPLANE
                , R.string.title_toggle_airplane, R.drawable.switch_airplane_on));
        BUTTONS.put(SwitchButton.BUTTON_AUTOROTATE, new ButtonInfo(
                SwitchButton.BUTTON_AUTOROTATE
                , R.string.title_toggle_autorotate, R.drawable.switch_orientation_on));
        BUTTONS.put(SwitchButton.BUTTON_BLUETOOTH, new ButtonInfo(
                SwitchButton.BUTTON_BLUETOOTH
                , R.string.title_toggle_bluetooth, R.drawable.switch_bluetooth_on));
        BUTTONS.put(SwitchButton.BUTTON_BRIGHTNESS, new ButtonInfo(
                SwitchButton.BUTTON_BRIGHTNESS
                , R.string.title_toggle_brightness, R.drawable.switch_brightness_on));
        BUTTONS.put(SwitchButton.BUTTON_TORCH, new ButtonInfo(
                SwitchButton.BUTTON_TORCH, R.string.title_toggle_flashlight, R.drawable.switch_torch_on));
        BUTTONS.put(SwitchButton.BUTTON_GPS, new ButtonInfo(
                SwitchButton.BUTTON_GPS, R.string.title_toggle_gps, R.drawable.switch_gps_on));
        BUTTONS.put(SwitchButton.BUTTON_LOCK_NOW, new ButtonInfo(
                SwitchButton.BUTTON_LOCK_NOW, R.string.title_toggle_locknow, R.drawable.switch_lock_now));
        BUTTONS.put(SwitchButton.BUTTON_DATA, new ButtonInfo(
                SwitchButton.BUTTON_DATA, R.string.title_toggle_mobiledata, R.drawable.switch_data_on));
        BUTTONS.put(SwitchButton.BUTTON_NETWORKMODE, new ButtonInfo(
                SwitchButton.BUTTON_NETWORKMODE, R.string.title_toggle_networkmode, R.drawable.switch_2g3g));
        BUTTONS.put(SwitchButton.BUTTON_SCREEN_OFF, new ButtonInfo(
                SwitchButton.BUTTON_SCREEN_OFF, R.string.title_toggle_sleep, R.drawable.switch_screen_off));
        BUTTONS.put(SwitchButton.BUTTON_SOUND, new ButtonInfo(
                SwitchButton.BUTTON_SOUND, R.string.title_toggle_sound, R.drawable.switch_sv_sound));
        BUTTONS.put(SwitchButton.BUTTON_SYNC, new ButtonInfo(
                SwitchButton.BUTTON_SYNC, R.string.title_toggle_sync, R.drawable.switch_sync_on));
        BUTTONS.put(SwitchButton.BUTTON_WIFI, new ButtonInfo(
                SwitchButton.BUTTON_WIFI, R.string.title_toggle_wifi, R.drawable.switch_wifi_on));
        BUTTONS.put(SwitchButton.BUTTON_WIFI_AP, new ButtonInfo(
                SwitchButton.BUTTON_WIFI_AP, R.string.title_toggle_wifiap, R.drawable.switch_wifi_ap_on));
        BUTTONS.put(SwitchButton.BUTTON_SHUTDOWN, new ButtonInfo(
                SwitchButton.BUTTON_SHUTDOWN, R.string.title_toggle_shutdown, R.drawable.switch_shutdown));
        BUTTONS.put(SwitchButton.BUTTON_REBOOT, new ButtonInfo(
                SwitchButton.BUTTON_REBOOT, R.string.title_toggle_reboot, R.drawable.switch_reboot));
        BUTTONS.put(SwitchButton.BUTTON_SWITCH_WIDGET_STYLE, new ButtonInfo(SwitchButton.BUTTON_SWITCH_WIDGET_STYLE
                , R.string.title_toggle_switchwidgetstyle, R.drawable.switch_widget_style_dual));
    }

    public String getCurrentButtons(boolean tinyMode) {
        String buttons = Settings.System.getString(
                getContext().getContentResolver()
                , tinyMode ? Settings.System.SWITCH_WIDGET_BUTTONS_TINY
                : Settings.System.SWITCH_WIDGET_BUTTONS);
        return buttons == null ? SwitchButton.BUTTONS_ALL : buttons;
    }

    public void saveCurrentButtons() {
        String str = getButtonStringFromList(true);
        if (!mCurrentTinyButtons.equals(str)) {
            Settings.System.putString(
                    getContext().getContentResolver(), Settings.System.SWITCH_WIDGET_BUTTONS_TINY, str);
        }

        str = getButtonStringFromList(false);
        if (!mCurrentButtons.equals(str)) {
            Settings.System.putString(
                    getContext().getContentResolver(), Settings.System.SWITCH_WIDGET_BUTTONS, str);
        }
    }

/*
 *     public String mergeInNewButtonString(String oldString, String newString) {
 *         ArrayList<String> newList = getButtonListFromString(newString);
 *         ArrayList<String> mergedList = getButtonListFromString(oldString);
 * 
 *         // append anything in newlist that isn't already in the merged list to the end of the list
 *         for(String button : newList) {
 *             if (!mergedList.contains(button)) {
 *                 mergedList.add(button);
 *             }
 *         }
 * 
 *         // return merged list
 *         // return getButtonStringFromList(mergedList, false);
 *     }
 */

    public ArrayList<String> getButtonListFromString(String buttons) {
        return new ArrayList<String>(Arrays.asList(buttons.split("\\|")));
    }

    public String getButtonStringFromList(boolean tinyMode) {
        if (mButtons == null || mButtons.size() <= 0) {
            return "";
        }

        if (tinyMode) {
            StringBuilder sb = new StringBuilder();
            ButtonInfo bi ;
            for (int i = 0; i < mButtons.size(); i++) {
                bi = mButtons.get(i);
                if (bi.isSelected()) {
                    sb.append(bi.getId());
                    sb.append(SwitchButton.BUTTON_DELIMITER);
                }
            }
            // if (DBG) {
                // Log.d(TAG, "Saved tiny buttons: " + sb.toString());
            // }

            // Switch toggle should always be selected
            if (sb.indexOf(SwitchButton.BUTTON_SWITCH_WIDGET_STYLE) == -1) {
                return sb.append(SwitchButton.BUTTON_SWITCH_WIDGET_STYLE).toString();
            }

            return sb.subSequence(0, sb.length()-SwitchButton.BUTTON_DELIMITER.length()).toString();
        } else {
            StringBuilder sb = new StringBuilder(mButtons.get(0).getId());
            for(int i = 1; i < mButtons.size(); i++) {
                sb.append(SwitchButton.BUTTON_DELIMITER);
                sb.append(mButtons.get(i).getId());
            }
            // if (DBG) {
                // Log.d(TAG, "Saved buttons: " + sb.toString());
            // }
            return sb.toString();
        }
    }

    public static class ButtonInfo {
        private String mId;
        private int mTitleResId;
        private int mIconResId;
        private boolean mSelected;

        public ButtonInfo(String id, int titleResId, int iconResId) {
            mId = id;
            mTitleResId = titleResId;
            mIconResId = iconResId;
        }

        public String getId() { return mId; }
        public int getTitleResId() { return mTitleResId; }
        public int getIconResId() { return mIconResId; }

        public boolean isSelected() { return mSelected; }
        public void setSelected(boolean selected) { mSelected = selected; }
    }
}
