package com.weather.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.weather.model.DarkSkyWrapper;
import com.weather.model.Geolocation;
import com.weather.model.Geolocation.Result.Geometry.Location;
import com.weather.repository.DarkSkyApiRepository;
import com.weather.repository.GoogleApiRepository;
import com.weather.repository.OpenWeatherApiRepository;
import com.weather.model.OpenWeatherWrapper;

@Service
public class PublicService {

    private static final Logger log = LoggerFactory.getLogger(PublicService.class);
    
    @Value("${api.api-key.open-weather}")
    private String OPEN_WEATHER_APPID;

    @Value("${api.format.open-weather}")
    private String OPEN_WEATHER_FORMAT;

    @Value("${api.api-key.dark-sky}")
    private String DARK_SKY_APPID;

    @Value("${api.api-key.google-maps}")
    private String GEOLOCATION_APPID;
    
    private GoogleApiRepository googleApiRepo;
    
    private OpenWeatherApiRepository openApiRepo;
    
    private DarkSkyApiRepository darkSkyRepo;
    
    @Autowired
    public void setGoogleApiRepo(GoogleApiRepository googleApiRepo) {
        this.googleApiRepo = googleApiRepo;
    }
    
    @Autowired
    public void setOpenApiRepo(OpenWeatherApiRepository openApiRepo) {
        this.openApiRepo = openApiRepo;
    }

    @Autowired
    public void setDarkSkyRepo(DarkSkyApiRepository darkSkyRepo) {
        this.darkSkyRepo = darkSkyRepo;
    }

    public void process(Model model, String cityName, String apiProvider) 
            throws JsonProcessingException, IOException {

        String url = getOpenWeatherUrl(cityName);

        if (apiProvider.equals("openWeather") && OPEN_WEATHER_FORMAT.equalsIgnoreCase("json")) {
            
            log.info("OpenWeather provider called with JSON");
            openWeatherProviderProcess(url, model, cityName);
        } else if (apiProvider.equals("openWeather") && OPEN_WEATHER_FORMAT.equalsIgnoreCase("xml")) {
            
            log.info("OpenWeather provider called with XML");
            openWeatherProviderProcessWithXml(url, model, cityName);
        } else if (apiProvider.equals("darkSky")) {
            
            log.info("DarkSky provider called");
            darkSkyProviderProcess(model, cityName);
        }
        
    }
    
    private void openWeatherProviderProcess(String url, Model model, String cityName)
            throws JsonProcessingException, IOException {

        OpenWeatherWrapper wrapper = openApiRepo.getInformationAndMapToObject(url);

        String imgUrl = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host("openweathermap.org")
                .path(String.format("/img/w/%s.png", wrapper.getWeatherList().get(0).getIcon()))
                .build().encode("UTF-8").toUriString();
                        
        String temp = wrapper.getMainInfromation().getTemp();
        String desc = wrapper.getWeatherList().get(0).getDescription();
        
        model.addAttribute("imgUrl", imgUrl);
        model.addAttribute("temp", temp);
        model.addAttribute("desc", desc);
        model.addAttribute("cityName", cityName);
        
        log.info(String.format("OpenWeatherProvider called: temp=[%s], desc=[%s],"
                + " cityName=[%s]", temp, desc, cityName));
    }
    
    private String openWeatherProviderProcessWithXml(String url, Model model, String cityName)
            throws JsonProcessingException, IOException {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        return "process";
    }
    
    private void darkSkyProviderProcess(Model model, String cityName)
            throws JsonProcessingException, IOException {
        Location location = getGeolocation(cityName);

        String url = UriComponentsBuilder.newInstance().scheme("https")
                .host("api.darksky.net").path("/forecast")
                .path("/" + DARK_SKY_APPID)
                .path(String.format("/%s,%s", location.getLatitude(), location.getLongitude()))
                .queryParam("units", "si")
                .build().encode("UTF-8").toUriString();

        DarkSkyWrapper wrapper = darkSkyRepo.getInformationAndMapToObject(url);
        String temp = wrapper.getCurrentWeather().getTemperature();
        String desc = wrapper.getHourWeather().getSummary();
        
        model.addAttribute("temp", temp);
        model.addAttribute("desc", desc);
        model.addAttribute("cityName", cityName);
        
        log.info(String.format("DarkSky API called and returned: temp=[%s], desc=[%s],"
                + " cityName=[%s]", temp, desc, cityName));
    }
    
    private Location getGeolocation(String cityName) throws JsonProcessingException, IOException {
        
        String url = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host("maps.googleapis.com")
                .path("/maps/api/geocode/json")
                .queryParam("address", cityName)
                .queryParam("key", GEOLOCATION_APPID)
                .build().encode("UTF-8").toUriString();
        
        Geolocation geolocation = googleApiRepo.getInformationAndMapToObject(url);
        
        log.info(String.format("Geolocation asked from Google Map API. Geolocation=[%s].", geolocation));
        return geolocation.getResults().get(0).getGeometry().getLocation();
    }
    
    private String getOpenWeatherUrl(String cityName) throws UnsupportedEncodingException {
        String url = UriComponentsBuilder.newInstance().scheme("http")
        .host("api.openweathermap.org").path("/data/2.5/weather")
        .queryParam("q", cityName)
        .queryParam("units", "metric")
        .queryParam("APPID", OPEN_WEATHER_APPID)
        .queryParam("mode", OPEN_WEATHER_FORMAT)
        .build().encode("UTF-8").toUriString();
        
        return url;
    }
    
}