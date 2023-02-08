package com.appcino.as.sqltoexcel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * 
 * @author Anshuman Stutya
 * Utility class for static methods.
 *
 */
public class ExportHelper {
	
	 public static Cell getCell(Sheet s, int row, int col) {
         Row r = s.getRow(row);
         if (r == null) {
           r = s.createRow(row);
         }
         Cell c = r.getCell(col);
         if (c == null) {
           c = r.createCell(col);
         }
         return c;
       }

  public static Cell getCell(Sheet s, String name) {
         String col = "";
         String row = "";
         while (name.length() > 0) {
           char a = name.charAt(0);
           name = name.substring(1);
           try {
             Integer.parseInt("" + a);
             row += a;
           } catch (NumberFormatException e) {
             col += a;
           }
         }
         int rowi = Integer.parseInt(row) - 1;
         col = col.toUpperCase();

         char cols;
         int colsi;
         if (col.length() ==1){
        		  cols = col.charAt(0);
        		  colsi = ((int) cols) - 65;
         } else if (col.length()==2){
        	  cols = col.charAt(0);
    		  colsi = ((int) cols) - 65;
    		 cols = col.charAt(1);
    		 colsi = (((int) cols) - 65) + ((colsi+1)*26);
         } else
         {
            	  cols = col.charAt(0);
        		  colsi = ((int) cols) - 65;
        		 cols = col.charAt(1);
        		 colsi = (((int) cols) - 65) + ((colsi+1)*26);
        		 cols = col.charAt(2);
        		 colsi = (((int) cols) - 65) + ((colsi+1)*26);
         }
         

         return getCell(s, rowi, colsi);
       }


}
