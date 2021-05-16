package com.my.unit.mvc.servlet;

import com.my.unit.mvc.annotation.MyAutowired;
import com.my.unit.mvc.annotation.MyController;
import com.my.unit.mvc.annotation.MyRequestMapping;
import com.my.unit.mvc.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 * @description：MVC框架的请求分发中转
 * @author：GJF
 * Date： 2018-06-16 18:12
 * Remark：继承HttpServlet，重写init方法、doGet、doPost方法
 */

public class MyDispatcherServlet extends HttpServlet {

    private Logger log = Logger.getLogger("init");

    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();
    /**
    * handlerMapping的类型可以自定义为Handler
    */
    private Map<String, Method> handlerMapping = new  HashMap<>();

    private Map<String, Object> controllerMap  =new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init();
        log.info("初始化MyDispatcherServlet");
        //1.加载配置文件，填充properties字段；
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.根据properties，初始化所有相关联的类,扫描用户设定的包下面所有的类
        doScanner(properties.getProperty("scanPackage"));

        //3.拿到扫描到的类,通过反射机制,实例化,并且放到ioc容器中(k-v  beanName-bean) beanName默认是首字母小写
        doInstance();

        // 4.自动化注入依赖
        doAutowired();

        //5.初始化HandlerMapping(将url和method对应上)
        initHandlerMapping();

        //doAutowired2();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 注释掉父类实现，不然会报错：405 HTTP method GET is not supported by this URL
        //super.doPost(req, resp);
        log.info("执行MyDispatcherServlet的doPost()");
        try {
            //处理请求
            doDispatch(req,resp);
        } catch (Exception e) {
            resp.getWriter().write("500!! Server Exception");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 注释掉父类实现，不然会报错：405 HTTP method GET is not supported by this URL
        //super.doGet(req, resp);
        log.info("执行MyDispatcherServlet的doGet()");
        try {
            //处理请求
            doDispatch(req,resp);
        } catch (Exception e) {
            resp.getWriter().write("500!! Server Exception");
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if(handlerMapping.isEmpty()){
            return;
        }
        String url =req.getRequestURI();
        String contextPath = req.getContextPath();
        url=url.replace(contextPath, "").replaceAll("/+", "/");
        // 去掉url前面的斜杠"/"，所有的@MyRequestMapping可以不用写斜杠"/"
        if(url.lastIndexOf('/')!=0){
            url=url.substring(1);
        }
        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 NOT FOUND!");
            log.info("404 NOT FOUND!");
            return;
        }
        Method method =this.handlerMapping.get(url);
        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        //获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();
        //保存参数值
        Object [] paramValues= new Object[parameterTypes.length];
        //方法的参数列表
        for (int i = 0; i<parameterTypes.length; i++){
            //根据参数名称，做某些处理
            String requestParam = parameterTypes[i].getSimpleName();
            if ("HttpServletRequest".equals(requestParam)){
                //参数类型已明确，这边强转类型
                paramValues[i]=req;
                continue;
            }
            if ("HttpServletResponse".equals(requestParam)){
                paramValues[i]=resp;
                continue;
            }
            if("String".equals(requestParam)){
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value =Arrays.toString(param.getValue()).replaceAll("[\\[\\]]", "").replaceAll(",\\s", ",");
                    paramValues[i]=value;
                }
            }
        }
        //利用反射机制来调用
        try {
            //第一个参数是method所对应的实例 在ioc容器中
            //method.invoke(this.controllerMap.get(url), paramValues);
            Object result = method.invoke(this.controllerMap.get(url), paramValues);
            if(result instanceof String){
                resp.getWriter().write( (String)result );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Description:  根据配置文件位置，读取配置文件中的配置信息，将其填充到properties字段
     * Params:
     * @param location: 配置文件的位置
     * return: void
     */
    private void  doLoadConfig(String location){

        //把web.xml中的contextConfigLocation对应value值的文件加载到流里面
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            if(resourceAsStream == null){
                resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("com.my.unit.mvc");
            }
            //用Properties文件加载文件里的内容
            log.info("读取" + location + "里面的文件");
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            //关流
            if(null != resourceAsStream){
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Description:  将指定包下扫描得到的类，添加到classNames字段中；
     * Params:
     * @param packageName: 需要扫描的包名
     * return: void
     */
    private void doScanner(String packageName) {
        log.info("do scanner packageName:"+ packageName);
        URL url  =this.getClass().getClassLoader().getResource("/"+packageName.replaceAll("\\.", "/"));
        if(url == null){
            return;
        }
        File dir = new File(url.getFile());
        if(dir.exists()){
            File[] fileList = dir.listFiles();
            if(fileList == null || fileList.length <= 0){
                return;
            }
            for (File file : fileList) {
                if(file.isDirectory()){
                    //递归读取包
                    doScanner(packageName+"."+file.getName());
                }else{
                    String className =packageName +"." +file.getName().replace(".class", "");
                    classNames.add(className);
                }
            }
        }

    }

    /**
     * Description:  将classNames中的类实例化，经key-value：类名（小写）-类对象放入ioc字段中
     * Params:
     * return: void
     */
    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            try {
                //把类搞出来,反射来实例化(只有加@MyController需要实例化)
                Class<?> clazz =Class.forName(className);
                if(clazz.isAnnotationPresent(MyController.class)){
                    ioc.put(toLowerFirstWord(clazz.getSimpleName()),clazz.newInstance());
                }else if(clazz.isAnnotationPresent(MyService.class)){
                    MyService myService=clazz.getAnnotation(MyService.class);
                    String beanName=myService.value();
                    if ("".equals(beanName.trim())){
                        beanName=toLowerFirstWord(clazz.getSimpleName());
                    }

                    Object instance= clazz.newInstance();
                    ioc.put(beanName,instance);
                    //返回接口类
                    Class[] interfaces = clazz.getInterfaces();
                    for (Class<?> i:interfaces){
                        ioc.put(i.getName(),instance);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Description:自动化的依赖注入
     * return: void
     */
    private void doAutowired(){

        if (ioc.isEmpty()){
            return;
        }
        for (Map.Entry<String,Object> entry : ioc.entrySet()){
            //包括私有的方法，在spring中没有隐私，@MyAutowired可以注入public、private字段
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields){
                if (!field.isAnnotationPresent(MyAutowired.class)){
                    continue;
                }
                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                //反射的对象在使用时应该取消 Java 语言访问检查
                field.setAccessible(true);
                try {
                    log.info("doAutowired:" + entry.getValue() + "," + ioc.get(beanName));
                    field.set(entry.getValue(),ioc.get(beanName));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    private void doAutowired2(){
        if (controllerMap.isEmpty()){
            return;
        }
        for (Map.Entry<String,Object> entry : controllerMap.entrySet()){
            //包括私有的方法，在spring中没有隐私，@MyAutowired可以注入public、private字段
            Field[] fields=entry.getValue().getClass().getDeclaredFields();
            for (Field field:fields){
                if (!field.isAnnotationPresent(MyAutowired.class)){
                    continue;
                }
                MyAutowired autowired= field.getAnnotation(MyAutowired.class);
                String beanName=autowired.value().trim();
                if ("".equals(beanName)){
                    beanName=field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Description:  初始化HandlerMapping(将url和method对应上)
     * Params:
     * return: void
     */
    private void initHandlerMapping(){
        if(ioc.isEmpty()){
            return;
        }
        try {
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                Class<?> clazz = entry.getValue().getClass();
                if(!clazz.isAnnotationPresent(MyController.class)){
                    continue;
                }

                //拼url时,是controller头的url拼上方法上的url
                String baseUrl ="";
                if(clazz.isAnnotationPresent(MyRequestMapping.class)){
                    MyRequestMapping annotation = clazz.getAnnotation(MyRequestMapping.class);
                    baseUrl=annotation.value();
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if(!method.isAnnotationPresent(MyRequestMapping.class)){
                        continue;
                    }
                    MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
                    String url = annotation.value();

                    url =(baseUrl + "/" + url).replaceAll("/+", "/");
                    handlerMapping.put(url,method);
                    log.info("initHandlerMapping col:"+url+","+method);
                    controllerMap.put(url,entry.getValue());
                    log.info("initHandlerMapping controllerMap:"+url+","+entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Description:  将字符串中的首字母小写
     * Params:
     */
    private String toLowerFirstWord(String name){
        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

}
