package org.hyperic.hq.context;

import java.util.Collection;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;




public class Bootstrap  {
    static ApplicationContext appContext;
    
    public static <T> T getBean(Class<T> beanClass)  {
        try {
            Collection<T> beans = appContext.getBeansOfType(beanClass).values();
            if(beans.isEmpty() && appContext.getParent() != null) {
                beans = appContext.getParent().getBeansOfType(beanClass).values();
            }
            T bean = beans.iterator().next();
            if (bean == null) {
                throw new IllegalArgumentException("Couldn't locate bean of " + beanClass + " type");
            }
            return bean;
        } catch (BeansException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    public static Object getBean(String name) {
        try {
            Object bean = appContext.getBean(name);
            if(bean == null && appContext.getParent() != null) {
                bean = appContext.getParent().getBean(name);
            }
            return bean;
        } catch (BeansException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    public static boolean hasAppContext() {
        return Bootstrap.appContext != null;
    }
    
    public static Resource getResource(String location) {
        return Bootstrap.appContext.getResource(location);
    }
}