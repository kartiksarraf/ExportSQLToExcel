package com.appcino.as.sqltoexcel;

public class TestCharacter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String name ="AAA1";		
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
         
         
         System.out.println("Row: " + row + " column : " + col);
         System.out.println("Row: " + rowi + " column : " + colsi);
	}

}
