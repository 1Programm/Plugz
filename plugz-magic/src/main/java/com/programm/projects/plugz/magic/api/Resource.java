package com.programm.projects.plugz.magic.api;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Resources.class)
public @interface Resource {

    int NOTFOUND_ERROR = 0;
    int NOTFOUND_IGNORE = 1;
    int NOTFOUND_CREATE = 2;

    int ONEXIT_DISCARD = 0;
    int ONEXIT_SAVE = 1;

    /**
     * @return a name representing this resource.
     * If the name is left empty it will try to find a suitable resource based on the class name.
     */
    String value() default "";

    /**
     * @return a path where the resource is located.
     * If the path is left empty it will search through runtime resources.
     */
    String path() default "";

    /**
     * @return a state of which {@link Resource#NOTFOUND_ERROR} will throw en exception if the resource is not found.
     * {@link Resource#NOTFOUND_IGNORE} will ignore it if the resource could not be found.
     * {@link Resource#NOTFOUND_CREATE} will create a new file if the resource could not be found.
     */
    int notfound() default NOTFOUND_ERROR;

    /**
     * @return a state of which {@link Resource#ONEXIT_SAVE} will save the resource when the environment is shutting down.
     * {@link Resource#ONEXIT_DISCARD} will discard all changes made to the resource at runtime.
     */
    int onexit() default ONEXIT_DISCARD;

    /**
     * @return a resource loader for this resource.
     * It will try to read a file at the specific path or a static runtime resource from name by default.
     */
    Class<? extends IResourceLoader> loader() default IResourceLoader.class;

    /**
     * @return a merging strategy to merge this resource to the resource defined before if multiple {@link Resource} annotations are defined.
     * It will use the default merger if {@link IResourceMerger} is set.
     */
    Class<? extends IResourceMerger> merger() default IResourceMerger.class;

}
