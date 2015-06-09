/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2015 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 */

package com.pentaho.di.trans.dataservice.optimization.cache;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.pentaho.di.trans.dataservice.DataServiceExecutor;
import com.pentaho.di.trans.dataservice.optimization.PushDownFactory;
import com.pentaho.di.trans.dataservice.optimization.PushDownOptTypeForm;
import org.pentaho.caching.api.PentahoCacheManager;
import org.pentaho.caching.api.PentahoCacheTemplateConfiguration;

import javax.cache.Cache;
import javax.cache.CacheException;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author nhudak
 */
public class ServiceCacheFactory implements PushDownFactory {
  public static final String CACHE_NAME = ServiceCache.class.getName();
  private final PentahoCacheManager cacheManager;

  private final ListeningExecutorService executorService;

  public ServiceCacheFactory( PentahoCacheManager cacheManager, ExecutorService executorService ) {
    this.cacheManager = cacheManager;
    this.executorService = MoreExecutors.listeningDecorator( executorService );
  }

  @Override public String getName() {
    return ServiceCache.NAME;
  }

  @Override public Class<ServiceCache> getType() {
    return ServiceCache.class;
  }

  @Override public ServiceCache createPushDown() {
    return new ServiceCache( this );
  }

  @Override public PushDownOptTypeForm createPushDownOptTypeForm() {
    return new ServiceCacheOptForm( this );
  }

  public ListeningExecutorService getExecutorService() {
    return executorService;
  }

  public Cache<CachedService.CacheKey, CachedService> getCache( ServiceCache serviceCache )
    throws CacheException {
    String templateName = serviceCache.getTemplateName();
    Map<String, PentahoCacheTemplateConfiguration> templates = cacheManager.getTemplates();

    Cache<CachedService.CacheKey, CachedService> cache = cacheManager.getCache(
      CACHE_NAME,
      CachedService.CacheKey.class,
      CachedService.class
    );

    if ( cache != null ) {
      return cache;
    } else {
      Preconditions.checkState( !Strings.isNullOrEmpty( templateName ),
        "Cache Template is not configured" );
      Preconditions.checkState( templates.containsKey( templateName ),
        "Cache Template is not configured", templateName );

      return templates.get( templateName ).createCache(
        CACHE_NAME,
        CachedService.CacheKey.class,
        CachedService.class
      );
    }
  }

  public ServiceObserver createObserver( DataServiceExecutor executor ) {
    return new ServiceObserver( executor );
  }

  public CachedServiceLoader createCachedServiceLoader( CachedService cachedService ) {
    return new CachedServiceLoader( cachedService, executorService );
  }

  public Iterable<String> getTemplates() {
    // TODO need better template filtering & selection
    return cacheManager.getTemplates().keySet();
  }
}
