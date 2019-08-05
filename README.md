xUnit Spring
=====

A Spring integration for xUnit

# 简介
集成xUnit和Spring。xUnit的详细介绍请前往[xUnit](https://github.com/hejiehui/xUnit)  
进程启动时会加载xUnit的配置文件，解析其中配置的Unit实现类并注册成Spring Bean。实现类即可以按正常的Bean一样使用，比如注入其它的Service等。   
**版本对应关系**  

| xUnit Spring | xUnit | Spring Framework |
| :----------: | :---: | :--------------: |
| 0.0.1        | 0.9.3 | 4.3.x + |

# 集成步骤
下载源码后使用maven打包安装到本地maven库

**添加POM依赖**
```xml
<dependency>
    <groupId>com.xrosstools</groupId>
    <artifactId>xunit-spring</artifactId>
    <version>0.0.1</version>
</dependency>
```

**添加Spring配置**

在工程中的Spring Java Configuration类上添加`@EnableXunit`注解，示例如下：
```java
@Configuration
@EnableXunit("xunit/calculator.xunit")
public class SpringLotteryDrawSample {
    
    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringLotteryDrawSample.class);
        context.close();
    }
}
```
注解的参数为xUnit配置文件的路径，可以配置多个。参数格式也支持`Spring Resources`接口的path格式，如例子中的path可写成classpath:xunit/*.xunit，具体可参考[Spring Resources](https://docs.spring.io/spring/docs/4.3.x/spring-framework-reference/html/resources.html)

**修改业务代码**

将原来使用`XunitFactory.load()`方法初始化`XunitFactory`替换为直接使用`@Resource`注入(也可使用`@Autowird`+`@Qualifier`方式注入)。  
原代码：
```java
public class LotteryDrawSample {

	public void calculate() throws Exception {
		XunitFactory f = XunitFactory.load("xunit/calculator.xunit");
		Processor p = f.getProcessor("Lottery Draw");
		LotteryDrawContext ctx = new LotteryDrawContext("Jerry", 100, "+");
		p.process(ctx);
	}

	public static void main(String[] args) throws Exception {
		LotteryDrawSample sample = new LotteryDrawSample();
		sample.calculate();
	}
}
```  
修改后：
```java
@Configuration
@EnableXunit("xunit/calculator.xunit")
public class SpringLotteryDrawSample {

    @Resource(name = "sample.Calculator")
    private XunitFactory factory;

    public void calculate() throws Exception{
        Processor processor = factory.getProcessor("Lottery Draw");
        LotteryDrawContext ctx = new LotteryDrawContext("Jerry", 100, "+");
        processor.process(ctx);
    }

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringLotteryDrawSample.class);
        SpringLotteryDrawSample bootstrap = context.getBean(SpringLotteryDrawSample.class);
        bootstrap.calculate();
        context.close();
    }
}
```
上面的代码也可修改成：
```java
@Configuration
@EnableXunit("xunit/calculator.xunit")
public class SpringLotteryDrawSample {
    /** 直接注入Processor */
    @Resource(name = "sample.Calculator:LotteryDraw")
    private Processor processor;

    public void calculate(){
        LotteryDrawContext ctx = new LotteryDrawContext("Jerry", 100, "+");
        processor.process(ctx);
    }

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringLotteryDrawSample.class);
        SpringLotteryDrawSample bootstrap = context.getBean(SpringLotteryDrawSample.class);
        bootstrap.calculate();
        context.close();
    }
}
```
集成后的Unit的实现类可以直接引用其它的Bean，举个例子：
```java
public class UsernamePasswordValidator implements Validator {
    /** 系统中原有的Service可直接注入 */
    @Autowired
    private UserService userService;

    @Override
    public boolean validate(Context context) {
        LoginContext ctx = (LoginContext)context;
        boolean result = userService.validatePassword(ctx.getUsername(), ctx.getPassword());
        ctx.setLoginSuccess(result);
        return result;
    }
}
```
# 原理及问题
###功能限制
1. xUnit配置文件修改后不支持自动加载  
因为所有Unit托管在Spring容器中，所以在配置文件修改后，需要用户代码主动调用`ApplicationContext.refresh()`来刷新Unit的Bean定义  
2. 同一个Node下的child需要有唯一的Name属性值，否则会导致Bean Name冲突   
3. 自定义`Chain`或`Branch`实现类    
   一般项目中都使用默认的Chain和Branch的实现，如果用户使用自定义实现类，则需要满足如下条件：  
   **Chain实现类** ：包含`public void setUnits(List<Unit>)`方法 或者 包含一个public的第一个参数类型是`List<Unit>`的构造函数。
   以上两种方法至少选一个，如果都提供会优先选择构造函数的方式注入Child Unit，都不提供初始化时会抛出`NotWritablePropertyException`。   
   **Branch实现类** : 包含`public void setUnitMap(Map<String, Unit>)`方法 或者 包含一个public的第1，2个参数类型分别是`Locator`和Map<String, Unit>的构造函数。
   以上两种方法至少选一个，如果都提供会优先选择构造函数的方式注入Child Unit，都不提供初始化时会抛出`NotWritablePropertyException`。
 

###获取Bean Name
+ `XunitFactory`的Bean Name  
  + 如果name属性不为空，则格式为 ${packageId}.${name}
  + 如果name属性为空，则会用配置文件path生成
    + 如果文件在classpath下，比如为xunit/sample.xunit，则对应的Factory的Bean Name为xunit.sample
    + 如果是外部文件，比如path为file:/home/admin/sample.xunit, 则Factory的Bean Name只取文件名，为sample。
  > 建议为Factory设置不同的name和packageId属性，可以生成规范的Bean Name  
+ `Unit`的Bean Name  
  格式为${parent bean name}:${name}，比如配置文件格式为：
  ```xml
  <xunit name="MyFlow" packageId="test">
   <units>
    <chain class="default" description="" name="My Chain 2" type="processor">
      <processor class="default" description="" module="" name="unit 1" reference="">
        <property key="showMessage" value="1"/>
      </processor>
    </chain>
   </units>
  </xunit>
  ```
   则Factory的Bean Name为"test.MyFlow"，Chain的Bean Name为"test.MyFlow:MyChain2"，Processor的Bean Name为"test.MyFlow:MyChain2:unit1"  


###问题
1. 需要给Unit的实现类加上@Component或者其派生注解吗？  
所有被扫描的xUnit配置文件中的Unit实现类都会自动注册为Bean，强烈不建议再添加`@Component`或其派生注解，可能会导致错误
2. Bean用法有什么区别  
就是普通的Spring Bean，正常使用就可以了





