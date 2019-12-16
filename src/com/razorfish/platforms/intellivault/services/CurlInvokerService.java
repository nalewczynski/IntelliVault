package com.razorfish.platforms.intellivault.services;

import com.razorfish.platforms.intellivault.exceptions.CurlException;
import com.razorfish.platforms.intellivault.services.pojo.AEMPackage;
import com.razorfish.platforms.intellivault.services.pojo.AEMResponse;

import java.io.File;
import java.util.List;

public interface CurlInvokerService {
    AEMResponse createPackage(final String host, final String credentials,
                              final String packageGroup, final String packageName,
                              final String[] paths) throws CurlException;

    AEMResponse buildPackage(final String host, final String credentials,
                             final String packagePath) throws CurlException;

    AEMResponse installPackage(final String host, final String credentials,
                               final String packagePath) throws CurlException;

    AEMResponse deletePackage(final String host, final String credentials,
                              final String packagePath) throws CurlException;

    AEMResponse downloadPackage(final String host, final String credentials,
                                final String packagePath, final String filePath) throws CurlException;

    List<AEMPackage> listAEMPackages(final String host, final String credentials) throws CurlException;

    AEMResponse uploadPackage(final String host, final String credentials,
                              final File file) throws CurlException;
}
