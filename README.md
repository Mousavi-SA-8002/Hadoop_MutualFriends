**مستندات پروژه Hadoop - تحلیل دوستان مشترک با MapReduce**

## 1. مقدمه
این پروژه شامل تحلیل داده‌های یک شبکه اجتماعی با استفاده از Hadoop و MapReduce است. هدف این پروژه یافتن دوستان مشترک بین کاربران و همچنین تحلیل‌های اضافی بر روی مجموعه داده‌های بزرگ است.

## 2. ساختار پروژه

### 2.1. کد منبع
- `MutualFriends.java`: پیاده‌سازی MapReduce برای یافتن دوستان مشترک بین کاربران.
- `MutualFriends.jar`: فایل اجرایی جهت اجرای Job روی Hadoop.

### 2.2. فایل‌های خروجی
- `part-r-00000`: فایل حاوی خروجی نهایی پس از اجرای Job.
- `_SUCCESS`: نشان‌دهنده اجرای موفق Job در Hadoop.
- چندین تصویر (`.png`): شامل اسکرین‌شات‌های مراحل اجرا و خروجی‌ها.

### 2.3. فایل‌های تنظیمات Hadoop
- `core-site.xml`: پیکربندی مسیرهای اصلی Hadoop.
- `hdfs-site.xml`: تنظیمات مربوط به HDFS و تعداد نسخه‌های داده.
- `mapred-site.xml`: مشخص کردن نوع اجرای MapReduce.
- `yarn-site.xml`: مدیریت منابع در YARN.

## 3. توضیح فایل‌های تنظیمات Hadoop

### 3.1. `core-site.xml`
این فایل شامل تنظیمات اصلی برای مسیرهای HDFS است. محتوای این فایل در پروژه شما به صورت زیر است:
```xml
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://localhost:9000</value>
    </property>
</configuration>
```
**توضیح:** مقدار `fs.defaultFS` مشخص می‌کند که سیستم فایل پیش‌فرض Hadoop روی `hdfs://localhost:9000` تنظیم شده است، که نشان می‌دهد داده‌ها روی یک کلاستر محلی اجرا خواهند شد.

### 3.2. `hdfs-site.xml`
این فایل مشخص می‌کند که HDFS چگونه داده‌ها را ذخیره کند. محتوای این فایل در پروژه شما:
```xml
<configuration>
    <property>
        <name>dfs.replication</name>
        <value>1</value>
    </property>
</configuration>
```
**توضیح:** مقدار `dfs.replication` تعداد نسخه‌های هر فایل را مشخص می‌کند. مقدار ۱ یعنی فقط یک کپی از داده‌ها ذخیره شود، که برای محیط‌های توسعه و تست مناسب است.

### 3.3. `mapred-site.xml`
این فایل نوع اجرای MapReduce را مشخص می‌کند. محتوای این فایل:
```xml
<configuration>
    <property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
    </property>
    <property>
        <name>yarn.app.mapreduce.am.env</name>
        <value>HADOOP_MAPRED_HOME=/usr/local/hadoop</value>
    </property>
    <property>
        <name>mapreduce.map.env</name>
        <value>HADOOP_MAPRED_HOME=/usr/local/hadoop</value>
    </property>
    <property>
        <name>mapreduce.reduce.env</name>
        <value>HADOOP_MAPRED_HOME=/usr/local/hadoop</value>
    </property>
</configuration>
```
**توضیح:** مقدار `mapreduce.framework.name` مشخص می‌کند که Hadoop باید از YARN برای اجرای MapReduce استفاده کند. همچنین مسیر `HADOOP_MAPRED_HOME` برای اجرای فرآیندهای Map و Reduce تنظیم شده است.

### 3.4. `yarn-site.xml`
این فایل پیکربندی YARN را تعیین می‌کند. محتوای این فایل:
```xml
<configuration>
    <property>
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
    </property>
</configuration>
```

**توضیح:** مقدار `yarn.nodemanager.aux-services` تعیین می‌کند که YARN چگونه داده‌های مربوط به MapReduce را مدیریت کند. این مقدار `mapreduce_shuffle` را فعال می‌کند، که انتقال داده بین Mapper و Reducer را بهینه می‌کند.

## 4. پیاده‌سازی MapReduce

### 4.1. Mapper

**کد:**

```java
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

public class MutualFriendsMapper extends Mapper<Object, Text, Text, Text> {
    private Text userPair = new Text();
    private Text friendsList = new Text();

    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        String[] line = value.toString().split("\\s+");
        if (line.length < 2) return;

        String user = line[0];
        String[] friends = line[1].split(",");

        for (String friend : friends) {
            String userPairKey = (Integer.parseInt(user) < Integer.parseInt(friend)) ? user + "," + friend : friend + "," + user;
            userPair.set(userPairKey);
            friendsList.set(line[1]);
            context.write(userPair, friendsList);
        }
    }
}
```

**توضیح Mapper:** این کلاس خطوط ورودی را می‌خواند، کاربر و دوستان او را استخراج کرده و کلیدهای `(UserA, UserB)` را ایجاد می‌کند تا به Reducer ارسال شود.

### 4.2. Reducer

**کد:**

```java
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MutualFriendsReducer extends Reducer<Text, Text, Text, Text> {
    private Text result = new Text();

    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        Set<String> set1 = new HashSet<>();
        Set<String> set2 = new HashSet<>();
        int count = 0;

        for (Text val : values) {
            if (count == 0) {
                set1.addAll(Arrays.asList(val.toString().split(",")));
            } else {
                set2.addAll(Arrays.asList(val.toString().split(",")));
            }
            count++;
        }

        set1.retainAll(set2);
        result.set(String.join(",", set1));
        context.write(key, result);
    }
}
```

**توضیح Reducer:** مقدارهای دریافتی از Mapper را دریافت کرده و دوستان مشترک بین کاربران را محاسبه می‌کند.


## 5. مشکلات و راه‌حل‌ها در راه‌اندازی Hadoop

### مشکل 1: عدم شروع NameNode
**مشکل:** هنگام راه‌اندازی Hadoop، ممکن است `namenode` اجرا نشود.
**راه‌حل:** بررسی کنید که مسیرهای `hdfs-site.xml` به درستی تنظیم شده باشند و با اجرای دستورات زیر NameNode را فرمت و راه‌اندازی کنید:
```bash
hdfs namenode -format
start-dfs.sh
```

### مشکل 2: عدم دسترسی به `localhost:9870`
**مشکل:** داشبورد Hadoop در مرورگر نمایش داده نمی‌شود.
**راه‌حل:** بررسی کنید که سرویس‌های `namenode` و `datanode` در حال اجرا باشند:
```bash
jps
```
اگر `NameNode` یا `DataNode` اجرا نشده بود، دستورات زیر را اجرا کنید:
```bash
start-dfs.sh
start-yarn.sh
```

### مشکل 3: خطای `Connection refused` هنگام اجرای Job
**مشکل:** هنگام اجرای Job ممکن است خطای اتصال به `localhost:9000` رخ دهد.
**راه‌حل:** بررسی کنید که `core-site.xml` مقدار `fs.defaultFS` را به درستی تنظیم کرده باشد. همچنین اطمینان حاصل کنید که NameNode در حال اجرا است.

### مشکل 4: کاهش سرعت پردازش روی داده‌های بزرگ
**مشکل:** هنگام اجرای Job روی داده‌های بزرگ، پردازش بسیار کند می‌شود.
**راه‌حل:**
1. افزایش مقدار RAM اختصاص‌یافته به YARN در `yarn-site.xml`:
```xml
<property>
    <name>yarn.scheduler.minimum-allocation-mb</name>
    <value>1024</value>
</property>
```
2. استفاده از `Combiner` در MapReduce برای کاهش ترافیک داده بین Mapper و Reducer.

## 6. نتیجه‌گیری
این پروژه نشان داد که چگونه Hadoop و MapReduce برای تحلیل داده‌های بزرگ به کار گرفته می‌شوند. همچنین مشکلات رایج در راه‌اندازی Hadoop بررسی و راه‌حل‌هایی برای بهبود عملکرد ارائه شد.