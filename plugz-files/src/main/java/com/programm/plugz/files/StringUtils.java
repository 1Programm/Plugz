package com.programm.plugz.files;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    public static int findNextOutsideComment(String s, int off, int end, String toFind, String openComment, String closeComment, boolean deepComment){
        int inComment = 0;
        boolean toggleComment = openComment.equals(closeComment);

        int i = off;
        while(i < end){
            if(inComment == 0) {
                if(s.startsWith(toFind, i)){
                    return i;
                }
                else if(s.startsWith(openComment, i)){
                    inComment++;
                    i += openComment.length();
                }
                else {
                    i++;
                }
            }
            else {
                if(deepComment && s.startsWith(openComment, i)){
                    inComment++;
                    i += openComment.length();
                }
                else if(toggleComment && s.startsWith(openComment, i)){
                    inComment--;
                    i += openComment.length();
                }
                else if(s.startsWith(closeComment, i)){
                    inComment--;
                    i += closeComment.length();
                }
                else {
                    i++;
                }
            }
        }

        return -1;
    }

    public static String[] splitOutsideComment(String s, int off, int end, String splitAt, String openComment, String closeComment, boolean deepComment){
        List<String> split = new ArrayList<>();
        int inComment = 0;
        boolean toggleComment = openComment.equals(closeComment);

        int last = off;
        int i = off;
        while(i < end){
            if(inComment == 0) {
                if(s.startsWith(splitAt, i)){
                    String sub = s.substring(last, i);
                    if(!sub.isEmpty()) split.add(sub);

                    i += splitAt.length();
                    last = i;
                }
                else if(s.startsWith(openComment, i)){
                    inComment++;
                    i += openComment.length();
                }
                else {
                    i++;
                }
            }
            else {
                if(deepComment && s.startsWith(openComment, i)){
                    inComment++;
                    i += openComment.length();
                }
                else if(toggleComment && s.startsWith(openComment, i)){
                    inComment--;
                    i += openComment.length();
                }
                else if(s.startsWith(closeComment, i)){
                    inComment--;
                    i += closeComment.length();
                }
                else {
                    i++;
                }
            }
        }

        if(last < end){
            split.add(s.substring(last, end));
        }

        return split.toArray(new String[0]);
    }

    public static int trimIndexForward(String s, int start, int end){
        for(int i=start;i<end;i++){
            if(Character.isWhitespace(s.charAt(i))){
                i++;
            }
            else {
                return i;
            }
        }

        return end;
    }

    public static int trimIndexBackward(String s, int start, int end){
        for(int i=end;i>=start;i--){
            if(Character.isWhitespace(s.charAt(i))){
                i++;
            }
            else {
                return i;
            }
        }

        return end;
    }

}
