package de.digitalcollections.iiif.myhymir;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;

import io.bdrc.auth.AuthProps;
import io.bdrc.pdf.presentation.exceptions.BDRCAPIException;

public class GeoLocation {

    private static final String DBLocation = AuthProps.getProperty("geolite_countryDB");
    private static DatabaseReader dbReader;
    public static final String GEO_CACHE_KEY = "GeoDB";
    private static final Logger log = LoggerFactory.getLogger(GeoLocation.class);

    public static String getCountryName(String ip) {
        try {
            dbReader = (DatabaseReader) ServerCache.getObjectFromCache("info", GEO_CACHE_KEY);
            if (dbReader == null) {
                File database = new File(DBLocation);
                dbReader = new DatabaseReader.Builder(database).build();
                ServerCache.addToCache("info", GEO_CACHE_KEY, dbReader);
            }
            InetAddress ipAddress = InetAddress.getByName(ip);
            log.info("IP = {}", ipAddress);
            CountryResponse response = dbReader.country(ipAddress);
            return response.getCountry().getName();
        } catch (IOException | BDRCAPIException | GeoIp2Exception e) {
            log.info("GeoLocation exception {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
