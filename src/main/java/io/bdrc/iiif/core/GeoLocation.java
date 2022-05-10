package io.bdrc.iiif.core;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;

import io.bdrc.auth.AuthProps;

public class GeoLocation {

    private static final String DBLocation = AuthProps.getProperty("geolite_countryDB");
    public static final String GEO_CACHE_KEY = "GeoDB";
    public static final String HEADER_NAME = "X-Real-IP";
    private static final Logger log = LoggerFactory.getLogger(GeoLocation.class);
    private static DatabaseReader dbReader = getDbReader();

    public static DatabaseReader getDbReader() {
        try {
            File database = new File(DBLocation);
            return new DatabaseReader.Builder(database).withCache(new JCSNodeCache()).build();
        } catch (IOException e) {
            log.error("getDbReader() could not be found");
            return null;
        }
    }

    public static String getCountryCode(String ip) {
        try {
            final InetAddress ipAddress = InetAddress.getByName(ip);
            final CountryResponse response = dbReader.country(ipAddress);
            return response.getCountry().getIsoCode();
        } catch (IOException | GeoIp2Exception e) {
            log.error("getCountryName() failed to find a country for the following IP¨:" + ip);
            return null;
        }
    }
}
