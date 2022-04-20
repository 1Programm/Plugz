package com.programm.plugz.api.utils;

public class StringUtils {

    public static String format(String message, String replaceStart, String replaceEnd, Object... args) {
        if(message == null || args == null || args.length == 0) return message;

        StringBuilder sb = new StringBuilder();

        int i=0;
        int last = 0;
        int index;

        while((index = message.indexOf(replaceStart, last)) != -1){
            sb.append(message, last, index);

            int replaceEndIndex = message.indexOf(replaceEnd, index + 1);
            if(replaceEndIndex == -1) break;

            last = index;

            if(index + replaceStart.length() + 1 < replaceEndIndex) {
                String between = message.substring(index + replaceStart.length(), replaceEndIndex);

                try {
                    int num = Integer.parseInt(between);
                    if(num >= args.length) continue;

                    if(args[num] != null) {
                        sb.append(args[num].toString());
                    }
                    else {
                        sb.append("null");
                    }
                }
                catch (NumberFormatException e){
                    continue;
                }
            }
            else {
                if(i >= args.length) break;

                if(args[i] != null) {
                    sb.append(args[i].toString());
                }
                else {
                    sb.append("null");
                }
                i++;
            }

            last = replaceEndIndex + replaceEnd.length();
        }

        sb.append(message, last, message.length());

        return sb.toString();
    }

}
