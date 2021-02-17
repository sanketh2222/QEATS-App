/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.ItemEntity;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.ItemRepository;
import com.crio.qeats.repositories.MenuRepository;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.print.DocFlavor.STRING;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;



@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {



  @Autowired
  private RedisConfiguration redisConfiguration;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  @Autowired
  private RestaurantRepository restaurantRepository;


  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objectives:
  // 1. Implement findAllRestaurantsCloseby.
  // 2. Remember to keep the precision of GeoHash in mind while using it as a key.
  // Check RestaurantRepositoryService.java file for the interface contract.
  // @Cacheable
  public List<Restaurant> findAllRestaurantsCloseBy1(Double latitude, Double longitude, 
      LocalTime currentTime,Double servingRadiusInKms) {

    // Restaurant restaurant = mongoTemplate
    // .findOne(query(where("latitude").is(latitude)), Restaurant.class);
    // Query q = new Query();
    List<Double> dist = new ArrayList<>();
    ModelMapper mapper = modelMapperProvider.get();
    List<Restaurant> restaurants1 = new ArrayList<>();
    // List<RestaurantEntity> restaurants =
    // mongoTemplate.findAll(RestaurantEntity.class);
    System.out.println("calling db function");
    long startTimeInMillis = System.currentTimeMillis();
    List<RestaurantEntity> restaurants = restaurantRepository.findAll();
    for (RestaurantEntity restaurant : restaurants) {
      if (isOpenNow(currentTime, restaurant)) {
        // restaurants1.add(mapper.map(restaurant, Restaurant.class));
        Double distance = GeoUtils.findDistanceInKm(latitude, longitude, restaurant.getLatitude(),
            restaurant.getLongitude());
        if (distance < servingRadiusInKms) {
          restaurants1.add(mapper.map(restaurant, Restaurant.class));
        }
        dist.add(distance);
        // mapper.map(restaurants1, Restaurant.class);
      }
    }
    // System.out.println(dist);
    // RestaurantRepository
    // restaurantRepository.findAll();

    // q.addCriteria(Criteria.where("latitutude").is(latitude).and("longitude").is(longitude));
    // q.getQueryObject();
    // Res
    // Restaurant restaurants1 = mongoTemplate.findOne(q, Restaurant.class);
    // GeoHash.geoHashStringWithCharacterPrecision(latitude, longitude,
    // numberOfCharacters);

    // mongoTemplate.find

    long endTimeInMillis = System.currentTimeMillis();
    System.out.println("Your function took :" + (endTimeInMillis - startTimeInMillis));
    return restaurants1;
  }

  public List<Restaurant> findAllRestaurantsCloseByWithoutCache(Double latitude, Double longitude, 
      LocalTime currentTime, Double servingRadiusInKms) {

    // Restaurant restaurant = mongoTemplate
    // .findOne(query(where("latitude").is(latitude)), Restaurant.class);
    // Query q = new Query();
    List<Double> dist = new ArrayList<>();
    ModelMapper mapper = modelMapperProvider.get();
    List<Restaurant> restaurants1 = new ArrayList<>();
    // List<RestaurantEntity> restaurants =
    // mongoTemplate.findAll(RestaurantEntity.class);
    System.out.println("calling db function");
    long startTimeInMillis = System.currentTimeMillis();
    List<RestaurantEntity> restaurants = restaurantRepository.findAll();
    for (RestaurantEntity restaurant : restaurants) {
      if (isOpenNow(currentTime, restaurant)) {
        // restaurants1.add(mapper.map(restaurant, Restaurant.class));
        Double distance = GeoUtils.findDistanceInKm(latitude, longitude, restaurant.getLatitude(),
            restaurant.getLongitude());
        if (distance < servingRadiusInKms) {
          restaurants1.add(mapper.map(restaurant, Restaurant.class));
        }
        dist.add(distance);
        // mapper.map(restaurants1, Restaurant.class);
      }
    }
    // System.out.println(dist);
    // RestaurantRepository
    // restaurantRepository.findAll();

    // q.addCriteria(Criteria.where("latitutude").is(latitude).and("longitude").is(longitude));
    // q.getQueryObject();
    // Res
    // Restaurant restaurants1 = mongoTemplate.findOne(q, Restaurant.class);
    // GeoHash.geoHashStringWithCharacterPrecision(latitude, longitude,
    // numberOfCharacters);

    // mongoTemplate.find

    long endTimeInMillis = System.currentTimeMillis();
    System.out.println("Your function took :" + (endTimeInMillis - startTimeInMillis));
    return restaurants1;
  }

  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {

    // List<Restaurant> restaurants = null;
    // TODO: CRIO_TASK_MODULE_REDIS
    // We want to use cache to speed things up. Write methods that perform the same
    // functionality,
    // but using the cache if it is present and reachable.
    // Remember, you must ensure that if cache is not present, the queries are
    // directed at the
    // database instead.
    JedisPool jedisPool = redisConfiguration.getJedisPool();
    Jedis jedis = jedisPool.getResource();
    GeoHash geoHash = GeoHash.withCharacterPrecision(latitude, longitude, 7);
    String restaurant = jedis.get(geoHash.toBase32());
    if (restaurant == null) {
      List<Restaurant> rest = findAllRestaurantsCloseByWithoutCache(latitude, longitude,
          currentTime,
          servingRadiusInKms);
      ObjectMapper obj = new ObjectMapper();
      try {
        jedis.set(geoHash.toBase32(), obj.writeValueAsString(rest));
      } catch (JsonProcessingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return rest;

    } else {
      List<Restaurant> restaurants = new ArrayList<>();
      try {
        Restaurant[] rests = new ObjectMapper().readValue(restaurant, Restaurant[].class);
        for (Restaurant rs: rests) {
          restaurants.add(rs);
        }
      } catch (JsonParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (JsonMappingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return restaurants;
    }


    //CHECKSTYLE:OFF
    //CHECKSTYLE:ON


    // return restaurants;
  }








  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objective:
  // 1. Check if a restaurant is nearby and open. If so, it is a candidate to be returned.
  // NOTE: How far exactly is "nearby"?

  /**
   * Utility method to check if a restaurant is within the serving radius at a given time.
   * @return boolean True if restaurant falls within serving radius and is open, false otherwise
   */
  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return GeoUtils.findDistanceInKm(latitude, longitude,
          restaurantEntity.getLatitude(), restaurantEntity.getLongitude())
          < servingRadiusInKms;
    }

    return false;
  }

  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
       String searchString,
      LocalTime currentTime, Double servingRadiusInKms) {
    // TODO Auto-generated method stub
    List<RestaurantEntity> restaurants = restaurantRepository.findAll();
    List<Restaurant> restaurant = new ArrayList<>();
    ModelMapper mapper = modelMapperProvider.get();
    for (RestaurantEntity r : restaurants) {
      if (r.getName().contains(searchString)) {
        if (isRestaurantCloseByAndOpen(r, currentTime,
            latitude, longitude, servingRadiusInKms)) {
          restaurant.add(mapper.map(r, Restaurant.class));
        }
        
      }
    }
    return restaurant;
  }

 

  @Override
  public List<Restaurant> findRestaurantsByAttributes(Double latitude, Double longitude,
      String searchString,
      LocalTime currentTime, Double servingRadiusInKms) {
    // TODO Auto-generated method stub
    List<RestaurantEntity> restaurants = restaurantRepository.findAll();
    Query q = new Query();
    String s = new String(searchString);
    q.addCriteria(Criteria.where("name").is(s));
    List<Restaurant> r = mongoTemplate.find(q, Restaurant.class);
    return r;
  }

  @Override
  public List<Restaurant> findRestaurantsByItemName(Double latitude, Double longitude,
      String searchString,
      LocalTime currentTime, Double servingRadiusInKms) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString,
      LocalTime currentTime, Double servingRadiusInKms) {
    // TODO Auto-generated method stub
    return null;
  }

 

  public static void main(String[] args) {

    RestaurantRepositoryServiceImpl r = new RestaurantRepositoryServiceImpl();
    r.findRestaurantsByAttributes(28.0, 77.0, "a", 
        LocalTime.of(18, 40), 3.0);
    // r.findAllRestaurantsCloseByWithoutCache(28.0, 77.0,
    //     LocalTime.of(18, 40), 3.0);
    
  }



}

