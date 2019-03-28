package com.wcq.springmvc.servlet;

import com.wcq.springmvc.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyDispatcherServlet extends HttpServlet {
    //存储aplication.properties 的配置内容
    private Properties properties = new Properties();
    //存储所有扫描到的类
    private List<String> classNames = new ArrayList<>();
    //IOC 容器，保存所有实例化对象
    private Map<String,Object> ioc = new HashMap<String,Object>();
    //保存Contrller 中所有Mapping 的对应关系
//    private Map<String, Method> handlerMapping = new HashMap<String,Method>();
    //Spring中通过List来存放HandlerMapping 所以此处使用Handler类来封装 uri 和Method，参数列表 controller等属性的对应
    private List<Handler> handlerMapping = new ArrayList<>();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        System.out.println("kKKK");
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatcher(req,resp);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    /**
     * 用于运行期，对用户请求的url进行Method匹配
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, InvocationTargetException, IllegalAccessException {
//        init(ServletConfig config);
//        String uri = req.getRequestURI();
//        System.out.println(uri);
//        String contextPath = req.getContextPath();
//        System.out.println(contextPath);
//        uri = uri.replace(contextPath,"").replaceAll("/+","/");
//        for (Map.Entry<String, Method> stringMethodEntry : handlerMapping.entrySet()) {
//            System.out.println(stringMethodEntry.getKey()+"++++++++++"+stringMethodEntry.getValue());
//        }
//
//        if(!handlerMapping.containsKey(uri)){
//            resp.getWriter().write("404 NOT FOUND");
//            return;
//        }
        Handler handler = getHandler(req);
        if(handler==null) {
            resp.getWriter().write("404 NOT FOUND");
            return;
        }
        //获得方法参数列表
        //Method method = this.handlerMapping.get(uri);
        Class<?>[] paramTypes = handler.getParamTypes();
        Object [] paramValues = new Object[paramTypes.length];

        Map<String,String[]> params = req.getParameterMap();
        for (Map.Entry<String, String[]> parm : params.entrySet()) {
            String value = Arrays.toString(parm.getValue()).replaceAll("\\[|\\]","")
                    .replaceAll("\\s",",");

            if(!handler.paramIndexMapping.containsKey(parm.getKey())){continue;}

            int index = handler.paramIndexMapping.get(parm.getKey());
            paramValues[index] = convert(paramTypes[index],value);
        }

        if(handler.paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
            int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;
        }

        if(handler.paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
            int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = resp;
        }

        Object returnValue = handler.method.invoke(handler.controller,paramValues);
        if(returnValue == null || returnValue instanceof Void){ return; }
        resp.getWriter().write(returnValue.toString());
//        Map<String, String[]> parameterMap = req.getParameterMap();



//        for (Map.Entry<String, String[]> stringEntry : parameterMap.entrySet()) {
//            System.out.println(stringEntry.getKey()+"------------------"+stringEntry.getValue()[0]);
//        }
//        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
//            System.out.println(entry.getKey()+">>>>>>>>>>>>>>"+entry.getValue());
//        }
//        System.out.println(method.toString());
//        String beanName = toLowerFirstName(method.getDeclaringClass().getSimpleName());
//        String beanName  = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
//        System.out.println(beanName);
//        System.out.println(ioc.get(beanName));
//        System.out.println(method.getDeclaringClass().toString());
//        System.out.println(method.getDeclaringClass().getSimpleName());
//        System.out.println(toLowerFirstName(method.getDeclaringClass().getSimpleName()));
//        Object[] obj = getObj(req, resp, method, parameterMap);
        //由于此处是传递的参数是写死的 所以需要优化
//        method.invoke(ioc.get(beanName),obj);
    }

    private Handler getHandler(HttpServletRequest req) {
        if(handlerMapping.isEmpty()){return null;}
        //绝对路径
        String url = req.getRequestURI();
        //处理成相对路径
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");
//        System.out.println(req.getRequestURI());
//        System.out.println(contextPath);
//        System.out.println("+++++++++++"+url);
        for (Handler handler : this.handlerMapping) {
            Matcher matcher = handler.getPattern().matcher(url);
            if(!matcher.matches()){ continue;}
//            System.out.println(handler.getParamTypes()+"EEEEEEEEEEEEEEE");
            return handler;
        }
        return null;
    }

    //url传过来的参数都是String类型的，HTTP是基于字符串协议
    //只需要把String转换为任意类型就好
    private Object convert(Class<?> type,String value){
        //如果是int
        if(Integer.class == type){
            return Integer.valueOf(value);
        }
        else if(Double.class == type){
            return Double.valueOf(value);
        }
        //如果还有double或者其他类型，继续加if
        //这时候，我们应该想到策略模式了
        //在这里暂时不实现，希望小伙伴自己来实现
        return value;
    }

    //自适应参数优化
    public Object [] getObj(HttpServletRequest req, HttpServletResponse resp,Method method,Map<String, String[]> parameterMap){
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] objects = new Object[parameterTypes.length];
        for (int i=0;i<parameterTypes.length;i++){
            Class<?> parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class){
                objects[i]=req;
                continue;
            }else if (parameterType == HttpServletResponse.class){
                objects[i]=resp;
                continue;
            }else if(parameterType == String.class){
                System.out.println(parameterType);
                MyRequestParam requestParam = parameterType.getAnnotation(MyRequestParam.class);
                System.out.println(requestParam);
                if(parameterMap.containsKey(requestParam.value())) {
                    for (Map.Entry<String,String[]> param : parameterMap.entrySet()){
                        String value = Arrays.toString(param.getValue())
                                .replaceAll("\\[|\\]","")
                                .replaceAll("\\s",",");
                        objects[i] = value;
                    }
                }
            }
        }
        return objects;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1、加载配置文件
        System.out.println(config.getInitParameter("contextConfigLocation"));
        loadConfig(config.getInitParameter("contextConfigLocation"));
        //2、扫描包下的类
        System.out.println(properties.getProperty("scanPackage")+"KKKKKKKKKKKKKKKKKK");
        scanPackage(properties.getProperty("scanPackage"));
        //3、创建类实例并存入IOC容器
        doInstance();
        //4、进行DI注入
        doAutowired();
        //5、初始化HandlerMapping
        initHandlerMapping();
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) return;
        for(Map.Entry<String,Object> entry:ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(MyController.class)) continue;
            String baseUrl = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)){
                MyRequestMapping myRequestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = myRequestMapping.value();
            }
            Method[] methods = clazz.getMethods();
            for(Method method : methods){
                if (!method.isAnnotationPresent(MyRequestMapping.class)) continue;
                MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
                String url = ("/"+baseUrl+"/"+annotation.value()).replaceAll("/+","/");
                Pattern pattern = Pattern.compile(url);
                System.out.println("__________"+pattern);
                handlerMapping.add(new Handler(pattern,entry.getValue(),method));
//                handlerMapping.put(url,method);
                System.out.println("Mapped " + url + "," + method);
            }
        }
    }

    private void doAutowired() {
        if(ioc.isEmpty()) return;
        //从ioc中的已初始化的实例中取出对应实例中的方法
        for(Map.Entry<String,Object> entry:ioc.entrySet()){
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field : fields){
                if(!field.isAnnotationPresent(MyAutowired.class)) continue;
                MyAutowired annotation = field.getAnnotation(MyAutowired.class);
                String beanName = annotation.value().trim();
                if("".equals(beanName)){
                    beanName= field.getType().getName();
                }
                //用于给私有属性打开访问权限
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doInstance() {
        if(classNames.isEmpty()) return;
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if(clazz.isAnnotationPresent(MyController.class)){
                    Object instance = clazz.newInstance();
                    String simpleName = toLowerFirstName(clazz.getSimpleName());
                    ioc.put(simpleName,instance);
                }else if(clazz.isAnnotationPresent(MyService.class)){
                    //1、默认的类名首字母小写
                    String beanName = toLowerFirstName(clazz.getSimpleName());
                    //2、自定义命名
                    MyService myService = clazz.getAnnotation(MyService.class);
                    String value = myService.value();
                    if(!"".equals(value.trim())){
                        beanName=value;
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);
                    //3、根据类型注入实现类，投机取巧的方式
                    for(Class<?> i : clazz.getInterfaces()){
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("The beanName is exists!!");
                        }
                        ioc.put(i.getName(),instance);
                    }
                }else{
                    continue;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
//        //初始化，为DI做准备
//        if(classNames.isEmpty()){return;}
//
//        try {
//            for (String className : classNames) {
//                Class<?> clazz = Class.forName(className);
//
//                //什么样的类才需要初始化呢？
//                //加了注解的类，才初始化，怎么判断？
//                //为了简化代码逻辑，主要体会设计思想，只举例 @Controller和@Service,
//                // @Componment...就一一举例了
//                if(clazz.isAnnotationPresent(MyController.class)){
//                    Object instance = clazz.newInstance();
//                    //Spring默认类名首字母小写
//                    String beanName = toLowerFirstName(clazz.getSimpleName());
//                    System.out.println(beanName+"*************************");
//                    ioc.put(beanName,instance);
//                }else if(clazz.isAnnotationPresent(MyService.class)){
//                    //1、自定义的beanName
//                    MyService service = clazz.getAnnotation(MyService.class);
//                    String beanName = service.value();
//                    //2、默认类名首字母小写
//                    if("".equals(beanName.trim())){
//                        beanName = toLowerFirstName(clazz.getSimpleName());
//                    }
//
//                    Object instance = clazz.newInstance();
//                    ioc.put(beanName,instance);
//                    //3、根据类型自动赋值,投机取巧的方式
//                    for (Class<?> i : clazz.getInterfaces()) {
//                        if(ioc.containsKey(i.getName())){
//                            throw new Exception("The “" + i.getName() + "” is exists!!");
//                        }
//                        //把接口的类型直接当成key了
//                        ioc.put(i.getName(),instance);
//                    }
//                }else {
//                    continue;
//                }
//
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }

    }

    private String toLowerFirstName(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0]+=32;
        return String.valueOf(chars);
    }

    private void scanPackage(String scanPackage) {
//        System.out.println(scanPackage);
//        System.out.println("/" + scanPackage.replaceAll("\\.", "/"));
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.","/"));
//        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
//        System.out.println(url);
        File classPath = new File(url.getFile());
        for(File file:classPath.listFiles()){
            if(file.isDirectory()){
                //递归 包路径要加上文件夹名
                scanPackage(scanPackage + "." + file.getName());
            }else{
                //不以.class文件的跳过
                if(!file.getName().endsWith(".class")) continue;
                //添加到类名列表中
                classNames.add(scanPackage+"."+file.getName().replace(".class",""));
            }
        }
    }

    private void loadConfig(String contextConfigLocation) {
        //this.class是获得这个类相对于Class类的对象
        //getClassLoader()是获得这个类对象的加载器
        InputStream resourceAsStream=null;
        try {
            resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
            properties.load(resourceAsStream);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if (null != resourceAsStream) {
                    resourceAsStream.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    //保存一个url和一个Method的关系
    public class Handler {
        //必须把url放到HandlerMapping才好理解吧
        private Pattern pattern;  //正则
        private Method method;
        private Object controller;
        private Class<?> [] paramTypes;

        public Pattern getPattern() {
            return pattern;
        }

        public Method getMethod() {
            return method;
        }

        public Object getController() {
            return controller;
        }

        public Class<?>[] getParamTypes() {
            return paramTypes;
        }

        //形参列表
        //参数的名字作为key,参数的顺序，位置作为值
        private Map<String,Integer> paramIndexMapping;

        public Handler(Pattern pattern, Object controller, Method method) {
            this.pattern = pattern;
            this.method = method;
            this.controller = controller;

            paramTypes = method.getParameterTypes();

            paramIndexMapping = new HashMap<String, Integer>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method){

            //提取方法中加了注解的参数
            //把方法上的注解拿到，得到的是一个二维数组
            //因为一个参数可以有多个注解，而一个方法又有多个参数
            Annotation[] [] pa = method.getParameterAnnotations();
            for (int i = 0; i < pa.length ; i ++) {
                for(Annotation a : pa[i]){
                    if(a instanceof MyRequestParam){
                        String paramName = ((MyRequestParam) a).value();
                        if(!"".equals(paramName.trim())){
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }

            //提取方法中的request和response参数
            Class<?> [] paramsTypes = method.getParameterTypes();
            for (int i = 0; i < paramsTypes.length ; i ++) {
                Class<?> type = paramsTypes[i];
                if(type == HttpServletRequest.class ||
                        type == HttpServletResponse.class){
                    paramIndexMapping.put(type.getName(),i);
                }
            }

        }
    }
}
