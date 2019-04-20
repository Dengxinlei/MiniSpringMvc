package com.gupao.stu.mvcframework.servlet;

import com.gupao.stu.mvcframework.annotation.MyAutowried;
import com.gupao.stu.mvcframework.annotation.MyController;
import com.gupao.stu.mvcframework.annotation.MyRequestMapping;
import com.gupao.stu.mvcframework.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class MyDispatchServlet extends HttpServlet {
    private static final Properties properties = new Properties();
    private static final List<String> beanNames = new ArrayList<String>();
    private static final Map<String, Object> ioc = new HashMap<String, Object>();
    private static final Map<String, Method> handleMapping = new HashMap<String, Method>();
    @Override
    public void init(ServletConfig config) {
        // 模板模式
        // 加载配置文件
        loadConfig(config.getInitParameter("contextConfigLocation"));
        // 初始化BeanName
        initBeanName();
        // 初始化ioc容器
        initIoc();
        // DI自动注入
        doAutowried();
        // 初始化HandleMapping容器
        initHandleMapping();
    }
    private void loadConfig(String configName) {
        try {
//            InputStream in = this.getClass().getClassLoader().getResourceAsStream(configName);
            InputStream in = MyDispatchServlet.class.getClassLoader().getResourceAsStream("applicationContext.properties");
            properties.load(in);
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
    }

    private void initBeanName() {
        String basePackage = properties.getProperty("base.package").replaceAll("\\.", "/");
        doScanPackage(basePackage);

    }

    private void doScanPackage(String basePackage) {
        File file = new File(this.getClass().getResource("/"+ basePackage).getFile());
        File[] files = file.listFiles();
        for(File f : files) {
            if(f.isFile()) {
                if(!f.getName().endsWith(".class")) { continue; }
                beanNames.add((basePackage+ "/" + f.getName()).replaceAll("/+", "."));
            } else {
                doScanPackage(basePackage+"/" + file.getName());
            }
        }


    }


    private void initIoc() {
        if(beanNames.isEmpty()) {
            return;
        }
        try {
            for (String beanName : beanNames) {
                Class clazz = Class.forName(beanName);
                String simpleName = toLowerFirstCase(clazz.getSimpleName());
                MyController controller = (MyController) clazz.getAnnotation(MyController.class);
                if(controller != null) {
                    if(controller.value() != null && !"".equals(controller.value())) {
                        simpleName = controller.value();
                    }
                    ioc.put(simpleName, clazz.newInstance());
                }
                MyService service = (MyService) clazz.getAnnotation(MyService.class);
                if(service != null) {
                    Class[] interfaces = clazz.getInterfaces();
                    for(Class iface : interfaces) {
                        if(!ioc.containsKey(toLowerFirstCase(iface.getSimpleName()))) {
                            ioc.put(toLowerFirstCase(iface.getSimpleName()), clazz.newInstance());
                        }
                    }
                }

            }
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
    }
    private String toLowerFirstCase(String beanNme) {
        if(beanNme == null || "".equals(beanNme)) {
            return null;
        }
        char[] chars = beanNme.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doAutowried() {
        if(ioc.isEmpty()) {
            return;
        }
        for(Map.Entry<String, Object> entry : ioc.entrySet()) {
            Object instance =  entry.getValue();
            Field[] fields = instance.getClass().getDeclaredFields();

            for(Field field : fields) {
                MyAutowried myAutowried = (MyAutowried)field.getAnnotation(MyAutowried.class);
                if(myAutowried != null) {
                    String iocName = toLowerFirstCase(field.getType().getSimpleName());
                    field.setAccessible(true);
                    try {
                        field.set(instance, ioc.get(iocName));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private void initHandleMapping() {
        if(ioc.isEmpty()) {
            return;
        }
        for(Map.Entry<String, Object> entry : ioc.entrySet()) {
            Object instance = entry.getValue();
            MyRequestMapping myRequestMapping = (MyRequestMapping) instance.getClass().getAnnotation(MyRequestMapping.class);
            String baseUri = null;
            if(myRequestMapping != null) {
                baseUri = myRequestMapping.value();
            }
            Method[] methods = instance.getClass().getMethods();
            for(Method method : methods) {
                myRequestMapping = (MyRequestMapping) method.getAnnotation(MyRequestMapping.class);
                if(myRequestMapping != null) {
                    handleMapping.put(baseUri + "/" + myRequestMapping.value(), method);
                }
            }
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        doPost(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        doDispatch(req, resp);
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {

    }
}
