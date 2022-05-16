package com.programm.plugz.inject;

import java.net.URL;

/**
 * A criteria which is used to determine if a specific class inside some url should match.
 */
public interface IClassCriteria {

    /**
     * The test method to tell if some class should be scanned.
     * @param url the url in which the class was discovered.
     * @param cls the class.
     * @return true if the class should be scanned.
     */
    boolean test(URL url, Class<?> cls);

}
