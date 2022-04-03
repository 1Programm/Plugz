package com.programm.projects.plugz.simple.webserv;

public class PrimitiveContentReader implements IContentReader {

    @Override
    public <T> T read(String content, Class<T> cls) throws ContentMapException {
        return tryPrimitive(content, cls);
    }

    @SuppressWarnings("unchecked")
    public static <T> T tryPrimitive(String content, Class<?> cls) throws ContentMapException {
        if(cls == String.class){
            return (T)content;
        }
        else if(cls == Byte.class || cls == Byte.TYPE){
            return (T)Byte.valueOf(content);
        }
        else if(cls == Short.class || cls == Short.TYPE){
            return (T)Short.valueOf(content);
        }
        else if(cls == Integer.class || cls == Integer.TYPE){
            return (T)Integer.valueOf(content);
        }
        else if(cls == Long.class || cls == Long.TYPE){
            return (T)Long.valueOf(content);
        }
        else if(cls == Float.class || cls == Float.TYPE){
            return (T)Float.valueOf(content);
        }
        else if(cls == Double.class || cls == Double.TYPE){
            return (T)Double.valueOf(content);
        }
        else if(cls == Character.class || cls == Character.TYPE){
            return (T)Character.valueOf(content.charAt(0));
        }

        return null;
    }
}
