package com.programm.plugz.inject;

import java.net.URL;

/**
 * A criteria which is used to determine if a specific class name inside some url is valid.
 * The class name represents a class with its full name.
 */
public interface INameCriteria {

    /**
     * The test method to tell if some class with that name should be scanned.
     * @param url the url in which the class was discovered.
     * @param fullClassName the full class name.
     * @return true if the class with that name should be scanned.
     */
    boolean test(URL url, String fullClassName);

}
