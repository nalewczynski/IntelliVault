package com.razorfish.platforms.intellivault.services.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorfish.platforms.intellivault.libs.CUrl;
import com.razorfish.platforms.intellivault.services.pojo.AEMPackage;
import com.razorfish.platforms.intellivault.services.pojo.AEMResponse;
import com.razorfish.platforms.intellivault.exceptions.CurlException;
import com.razorfish.platforms.intellivault.services.CurlInvokerService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CurlInvokerServiceImpl implements CurlInvokerService {

    private final static String URL_SERVICE = "/crx/packmgr/service/.json";
    private final static String URL_LIST = "/crx/packmgr/list.jsp";
    private final static String SUFFIX_FILTERS = "/jcr:content/vlt:definition/";
    private final static String CMD_CREATE = "?cmd=create";
    private final static String CMD_BUILD = "?cmd=build";
    private final static String CMD_INSTALL = "?cmd=install";
    private final static String CMD_DELETE = "?cmd=delete";
    private final static String CMD_UPLOAD = "?cmd=upload";

    public AEMResponse createPackage(final String host, final String credentials,
                                     final String packageGroup, final String packageName,
                                     final String[] paths) throws CurlException {
        try {
            final String packagePath = "/etc/packages/" + packageGroup + "/" + packageName + ".zip";

            CUrl curl = new CUrl(host + URL_SERVICE + packagePath + CMD_CREATE)
                    .opt("-X", "POST")
                    .data("packageName=" + packageName)
                    .data("groupName=" + packageGroup)
                    .opt("-u", credentials);

            curl.exec();

            AEMResponse aemResponse = parseAEMResponse(curl);

            if (!aemResponse.isSuccess()) {
                return aemResponse;
            }

            aemResponse.setSuccess(addFilters(host, credentials, packagePath, paths));
            return aemResponse;
        } catch (Exception e) {
            throw new CurlException("error while creating package", e);
        }
    }

    public AEMResponse buildPackage(final String host, final String credentials,
                                    final String packagePath) throws CurlException {
        try {
            CUrl curl = new CUrl(host + URL_SERVICE + packagePath + CMD_BUILD)
                    .opt("-X", "POST")
                    .opt("-u", credentials);
            curl.exec();
            return parseAEMResponse(curl);
        } catch (Exception e) {
            throw new CurlException("error while building package", e);
        }
    }

    public AEMResponse installPackage(final String host, final String credentials,
                                      final String packagePath) throws CurlException {
        try {
            CUrl curl = new CUrl(host + URL_SERVICE + packagePath + CMD_INSTALL)
                    .opt("-X", "POST")
                    .opt("-u", credentials);
            curl.exec();
            return parseAEMResponse(curl);
        } catch (Exception e) {
            throw new CurlException("error while installing package", e);
        }
    }

    public AEMResponse deletePackage(final String host, final String credentials,
                                     final String packagePath) throws CurlException {
        try {
            CUrl curl = new CUrl(host + URL_SERVICE + packagePath + CMD_DELETE)
                    .opt("-X", "POST")
                    .opt("-u", credentials);
            curl.exec();
            return parseAEMResponse(curl);
        } catch (Exception e) {
            throw new CurlException("error while installing package", e);
        }
    }

    public AEMResponse downloadPackage(final String host, final String credentials,
                                       final String packagePath, final String filePath) throws CurlException {
        try {
            CUrl curl = new CUrl(host + packagePath)
                    .opt("-X", "GET")
                    .opt("-u", credentials)
                    .output(new CUrl.FileIO(filePath));
            curl.exec();
            return parseAEMResponseFileDownload(curl);
        } catch (Exception e) {
            throw new CurlException("error while downloading package", e);
        }
    }

    public List<AEMPackage> listAEMPackages(final String host, final String credentials) throws CurlException {
        try {
            CUrl curl = new CUrl(host + URL_LIST)
                    .opt("-X", "GET")
                    .opt("-u", credentials);
            curl.exec();
            return parseAEMPackages(curl);
        } catch (IOException io) {
            throw new CurlException("error while parsing response to JAVA objects", io);
        }
    }

    public AEMResponse uploadPackage(final String host, final String credentials,
                                     final File file) throws CurlException {
        try {
            CUrl curl = new CUrl(host + URL_SERVICE + CMD_UPLOAD)
                    .form("package", new CUrl.FileIO(file))
                    .form("force", true + "")
                    .opt("-u", credentials);
            curl.exec();
            return parseAEMResponse(curl);
        } catch (Exception e) {
            throw new CurlException("error while creating package", e);
        }
    }

    private boolean addFilters(final String host, final String credentials,
                               final String packagePath, final String[] paths) throws CurlException {
        try {
            boolean success = true;
            CUrl curl = new CUrl(host + packagePath + SUFFIX_FILTERS)
                    .opt("-X", "POST")
                    .form("jcr:primaryType", "nt:unstructured")
                    .form(":nameHint", "filter")
                    .opt("-u", credentials);
            curl.exec();

            if (curl.getHttpCode() < 200 || curl.getHttpCode() >= 300) {
                success = false;
            }

            for (int i = 0; i < paths.length; i++) {
                curl = new CUrl(host + packagePath + SUFFIX_FILTERS + "/filter/")
                        .opt("-X", "POST")
                        .form("jcr:primaryType", "nt:unstructured")
                        .form("root", paths[i])
                        .form("mode", "replace")
                        .form(":nameHint", "f" + i)
                        .opt("-u", credentials);
                curl.exec();

                if (curl.getHttpCode() < 200 || curl.getHttpCode() >= 300) {
                    success = false;
                }
            }

            return success;
        } catch (Exception e) {
            throw new CurlException("error while adding filters", e);
        }
    }

    private AEMResponse parseAEMResponse(final CUrl curl) {
        final String strResponse = curl.getStdout(new CUrl.ToStringResolver("UTF-8"), null);
        final AEMResponse aemResponse = new AEMResponse(strResponse, curl.getHttpCode());
        try {
            aemResponse.setJsonResponse(new JSONObject(strResponse));
            aemResponse.setSuccess(aemResponse.getJsonResponse().getBoolean("success"));
            aemResponse.setExecutionTime(curl.getExecTime());

            if (aemResponse.getJsonResponse().has("path")) {
                aemResponse.setPath(aemResponse.getJsonResponse().getString("path"));
            }
        } catch (Exception e) {
            aemResponse.setSuccess(false);
        }

        return aemResponse;
    }

    private AEMResponse parseAEMResponseFileDownload(final CUrl curl) {
        final AEMResponse aemResponse = new AEMResponse("", curl.getHttpCode());
        aemResponse.setSuccess((curl.getHttpCode() >= 200 && curl.getHttpCode() < 300));
        aemResponse.setExecutionTime(curl.getExecTime());
        return aemResponse;
    }

    private List<AEMPackage> parseAEMPackages(final CUrl curl) throws IOException {
        final String strResponse = curl.getStdout(new CUrl.ToStringResolver("UTF-8"), null);
        final JSONObject jsonObject = new JSONObject(strResponse);

        final List<AEMPackage> aemPackages = new ArrayList<AEMPackage>();

        if (!jsonObject.has("results") || jsonObject.getJSONArray("results").length() == 0) {
            return aemPackages;
        }

        final JSONArray jResults = jsonObject.getJSONArray("results");

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        List<AEMPackage> list = objectMapper.readValue(jResults.toString(), new TypeReference<List<AEMPackage>>() {
        });
        return list;
    }

}
