package com.android.internal.policy.impl.lewa.view;

import java.io.IOException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;


public class Utils {

    private static final String TAG = "Utils";
    
    public static class Point
    {

        public void Offset(Point point)
        {
            double dx = x;
            double dxx = point.x;
            
            x = dx + dxx;
            
            double dy = y;
            double dyy = point.y;
            
            y = dy + dyy;
        }

        Point minus(Point point)
        {
            double dx = x;
            double dxx = point.x;
            
            double dy = y;
            double dyy = point.y;
            
            return new Point(dx - dxx, dy - dyy);
        }

        public double x;
        public double y;

        public Point(double dx, double dy)
        {
            x = dx;
            y = dy;
        }
    }
    
    
    /**
     * 计算两点之前的距离
     * @param point
     * @param point1
     * @param flag
     * @return
     */
    public static double Dist(Point point1, Point point2, boolean flag)
    {
        double p1_x = point1.x;
        double p2_x = point2.x;
        
        double p1_p2_x = p1_x - p2_x;
        
        double p1_y = point1.y;
        double p2_y = point2.y;
        
        double P1_p2_y = p1_y - p2_y;
        
        double x_square = p1_p2_x * p1_p2_x;
        double y_square = P1_p2_y * P1_p2_y;
        
        double d8;
        if(flag)
        {
            
            d8 = Math.sqrt(x_square + y_square);
        } else
        {
            d8 = x_square + y_square;
        }
        return d8;
    }
    
    public static void asserts(boolean flag) throws DomParseException{
        asserts(flag, "assert error");
    }

    public static void asserts(boolean flag, String exception)throws DomParseException{
        if(!flag){
            Log.e(TAG, exception);
        }
    }
    
    public static Element getChild(Element element, String tagName){
    
        if(element == null){
            return null;
        }
        NodeList nodelist = element.getChildNodes();
        
        Element childElement = null;
        
        int count = nodelist.getLength();
        for(int i=0;i<count;i++){
            Node node = nodelist.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equalsIgnoreCase(tagName)){
                childElement = (Element)node;
                break;
            }
        }
        return childElement;
    }
    
    public static int getAttrAsIntThrows(Element element, String numberWidth) throws DomParseException{

        int width = 0;
        try {
            width = Integer.parseInt(element.getAttribute(numberWidth));
        }catch(NumberFormatException numberformatexception){
            Object aobj[] = new Object[2];
            aobj[0] = numberWidth;
            aobj[1] = element.toString();
            throw new DomParseException(String.format("fail to get attribute name: %s of Element %s", aobj));
        }
        return width;
    }
    
    public static int getAttrAsInt(Element element, String attriName, int defaultValue){
    
        try {
            return Integer.parseInt(element.getAttribute(attriName));
        } catch (NumberFormatException e) {
            return defaultValue;
        }

    }
    
    public static boolean equals(Object obj, Object obj1){
    
        if(obj != obj1 && (obj == null || !obj.equals(obj1))){
            return false;
        }else {
            return true;
        }
    }
    
    public static Point pointProjectionOnSegment(Point point, Point point1, Point point2, boolean flag)
    {
        Point point3;
        double d8;
        point3 = point1.minus(point);
        Point point4 = point2.minus(point);
        double d = point3.x;
        double d1 = point4.x;
        double d2 = d * d1;
        double d3 = point3.y;
        double d4 = point4.y;
        double d5 = d3 * d4;
        double d6 = d2 + d5;
        double d7 = Dist(point, point1, false);
        d8 = d6 / d7;
        if(d8 >= 0D && d8 <= 1D) {
            double d9 = point3.x * d8;
            double d10 = point3.y * d8;
            
            Point point5 = new Point(d9, d10);
            
            point5.Offset(point);
            return point5;
        } else {
          if(flag){
              if (d8 >= 0D) {
                  point = point1;
              } /*else {
                 point = null;
              }*/
          }  
        }
        return point;

    }

    public static boolean runShellBat(String commond){
        Runtime runtime = Runtime.getRuntime();
        String[] commonds = new String[3];
        commonds[0] = "sh";
        commonds[1] = "-c";
        commonds[2] = commond;
        try {
            Process process = runtime.exec(commonds);
            int exitValue = process.waitFor();
            if(exitValue != 0){
                return true;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }
	
}
