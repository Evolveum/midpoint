/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class ClasspathUrlFinder {
    
    public static final String code_id = "$Id$";
    /**
    * Find the classpath URLs for a specific classpath resource.  The classpath URL is extracted
    * from loader.getResources() using the baseResource.
    *
    * @param baseResource
    * @return
    */
   public static URL[] findResourceBases(String baseResource, ClassLoader loader)
   {
      ArrayList<URL> list = new ArrayList<URL>();
      try
      {
         Enumeration<URL> urls = loader.getResources(baseResource);
         while (urls.hasMoreElements())
         {
            URL url = urls.nextElement();
            list.add(findResourceBase(url, baseResource));
         }
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      return list.toArray(new URL[list.size()]);
   }

   /**
    * Find the classpath URLs for a specific classpath resource.  The classpath URL is extracted
    * from loader.getResources() using the baseResource.
    *
    * @param baseResource
    * @return
    */
   public static URL[] findResourceBases(String baseResource)
   {
      return findResourceBases(baseResource, Thread.currentThread().getContextClassLoader());
   }

   private static URL findResourceBase(URL url, String baseResource)
   {
      String urlString = url.toString();
      int idx = urlString.lastIndexOf(baseResource);
      urlString = urlString.substring(0, idx);
      URL deployUrl = null;
      try
      {
         deployUrl = new URL(urlString);
      }
      catch (MalformedURLException e)
      {
         throw new RuntimeException(e);
      }
      return deployUrl;
   }

   /**
    * Find the classpath URL for a specific classpath resource.  The classpath URL is extracted
    * from Thread.currentThread().getContextClassLoader().getResource() using the baseResource.
    *
    * @param baseResource
    * @return
    */
   public static URL findResourceBase(String baseResource)
   {
      return findResourceBase(baseResource, Thread.currentThread().getContextClassLoader());
   }

   /**
    * Find the classpath URL for a specific classpath resource.  The classpath URL is extracted
    * from loader.getResource() using the baseResource.
    *
    * @param baseResource
    * @param loader
    * @return
    */
   public static URL findResourceBase(String baseResource, ClassLoader loader)
   {
      URL url = loader.getResource(baseResource);
      return findResourceBase(url, baseResource);
   }

   /**
    * Find the classpath for the particular class
    *
    * @param clazz
    * @return
    */
   public static URL findClassBase(Class clazz)
   {
      String resource = clazz.getName().replace('.', '/') + ".class";
      return findResourceBase(resource, clazz.getClassLoader());
   }

   /**
    * Uses the java.class.path system property to obtain a list of URLs that represent the CLASSPATH
    *
    * @return
    */
   public static URL[] findClassPaths()
   {
      List<URL> list = new ArrayList<URL>();
      String classpath = System.getProperty("java.class.path");
      StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);

      while (tokenizer.hasMoreTokens())
      {
         String path = tokenizer.nextToken();
         File fp = new File(path);
         if (!fp.exists()) throw new RuntimeException("File in java.class.path does not exist: " + fp);
         try
         {
            list.add(fp.toURL());
         }
         catch (MalformedURLException e)
         {
            throw new RuntimeException(e);
         }
      }
      return list.toArray(new URL[list.size()]);
   }

   /**
    * Uses the java.class.path system property to obtain a list of URLs that represent the CLASSPATH
    * <p/>
    * paths is used as a filter to only include paths that have the specific relative file within it
    *
    * @param paths comma list of files that should exist in a particular path
    * @return
    */
   public static URL[] findClassPaths(String... paths)
   {
      ArrayList<URL> list = new ArrayList<URL>();

      String classpath = System.getProperty("java.class.path");
      StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);
      for (int i = 0; i < paths.length; i++)
      {
         paths[i] = paths[i].trim();
      }

      while (tokenizer.hasMoreTokens())
      {
         String path = tokenizer.nextToken().trim();
         boolean found = false;
         for (String wantedPath : paths)
         {
            if (path.endsWith(File.separator + wantedPath))
            {
               found = true;
               break;
            }
         }
         if (!found) continue;
         File fp = new File(path);
         if (!fp.exists()) throw new RuntimeException("File in java.class.path does not exists: " + fp);
         try
         {
            list.add(fp.toURL());
         }
         catch (MalformedURLException e)
         {
            throw new RuntimeException(e);
         }
      }
      return list.toArray(new URL[list.size()]);
   }

}
