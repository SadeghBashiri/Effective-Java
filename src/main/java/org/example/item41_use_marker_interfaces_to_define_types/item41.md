<div dir="rtl">

<a id="top"></a>

# آیتم ۴۱: استفاده از Marker Interface برای تعریف نوع (Type)

## (Use marker interfaces to define types)

در این آیتم از کتاب Effective Java، Joshua Bloch به یک سؤال مهم در طراحی APIهای جاوا پاسخ می‌دهد:

وقتی می‌خواهیم مشخص کنیم یک کلاس ویژگی خاصی دارد، اما نیازی به اضافه‌کردن هیچ متد جدیدی به آن نداریم، بهتر است از یک `interface` خالی استفاده کنیم یا از یک annotation؟

---

## فهرست مطالب

- [۱. Marker Interface دقیقاً چیست؟](#what-is-marker)
- [۲. چرا Marker Interface با Marker Annotation یکسان نیست؟](#marker-vs-annotation)
- [۳. مهم‌ترین مزیت Marker Interface: بررسی نوع در زمان کامپایل](#compile-time-check)
  - [یک نکتهٔ ظریف دربارهٔ `Serializable` واقعی در Java](#serializable-note)
- [۴. مزیت دوم: محدودکردن دقیق‌تر محل استفاده](#type-restriction)
  - [همین کار با Annotation چه تفاوتی دارد؟](#annotation-difference)
- [۵. آیا `Set` هم یک Marker Interface است؟](#set-marker)
- [۶. پس Marker Annotation چه مزیتی دارد؟](#annotation-advantage)
- [۷. چطور بین Marker Interface و Marker Annotation انتخاب کنیم؟](#how-to-choose)
- [۸. ارتباط Item 41 با Item 22](#connection-item22)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="what-is-marker"></a>
## ۱. Marker Interface دقیقاً چیست؟

یک Marker Interface اینترفیس‌ای است که هیچ متدی تعریف نمی‌کند و هدفش این است که نشان دهد کلاسی که آن را پیاده‌سازی کرده، ویژگی مشخصی دارد.

مثال معروف در Java، اینترفیس `java.io.Serializable` است:

<div dir="ltr">

```java
import java.io.Serializable;

public class User implements Serializable {
    private String username;
    private int age;
}
```
</div>

اینترفیس `Serializable` متدی ندارد که کلاس `User` مجبور باشد پیاده‌سازی کند. بااین‌حال، پیاده‌سازی آن اعلام می‌کند که نمونه‌های این کلاس برای فرایند serialization واجد شرایط هستند.

> **نکتهٔ مهم:** اینترفیس خالی، همچنان یک **نوع (Type) واقعی** در سیستم نوع‌های Java است.

<div dir="ltr">

```java
Serializable user = new User();   // ✅ معتبر
Serializable value = new Object(); // ❌ Compile Error
```
</div>

پس حتی بدون داشتن هیچ متدی، اینترفیس می‌تواند روی معتبر بودن یا نبودن یک انتساب در زمان کامپایل تأثیر بگذارد.

[بازگشت به بالا](#top)

---

<a id="marker-vs-annotation"></a>
## ۲. چرا Marker Interface با Marker Annotation یکسان نیست؟

فرض کن به‌جای اینترفیس، از annotation استفاده کنیم:

<div dir="ltr">

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SerializableMarker {
}

@SerializableMarker
public class User { }
```
</div>

این کلاس اکنون علامت‌گذاری شده است، اما این علامت‌گذاری آن را به یک نوع جدید تبدیل نمی‌کند:

<div dir="ltr">

```java
SerializableMarker value = new User();  // ❌ Compile Error
```
</div>

چون `User` یک annotation را دریافت کرده، نه اینکه آن را به‌عنوان یک interface پیاده‌سازی کرده باشد.

| ویژگی | Marker Interface | Marker Annotation |
|--------|------------------|-------------------|
| هدف | مشخص‌کردن یک نوع | افزودن metadata |
| ایجاد رابطهٔ نوعی | بله | خیر |
| بررسی عضویت با type checking در زمان کامپایل | بله | به‌صورت مستقیم خیر |
| امکان استفاده به‌عنوان نوع پارامتر | بله | خیر |
| دسترسی به علامت در زمان اجرا | از طریق نوع و reflection | از طریق annotation و reflection |

> **نتیجه:** Marker Interface بخشی از **سیستم نوع‌های Java** است، اما Marker Annotation بخشی از **سیستم metadata** این زبان است.

[بازگشت به بالا](#top)

---

<a id="compile-time-check"></a>
## ۳. مهم‌ترین مزیت Marker Interface: بررسی نوع در زمان کامپایل

نویسنده می‌گوید بزرگ‌ترین مزیت Marker Interface این است که می‌تواند خطاهایی را پیش از اجرای برنامه آشکار کند.

<div dir="ltr">

```java
public void process(Serializable data) {
    // Process data
}

process(new User());    // ✅ اگر User implements Serializable باشد
process(new Object());  // ❌ Compile Error
```
</div>

کامپایلر می‌تواند از روی نوع پارامتر تشخیص دهد که آیا شیء ورودی واجد شرایط است یا خیر.

این همان مزیتی است که با annotation به دست نمی‌آید. اگر به‌جای interface از `@SerializableMarker` استفاده کنیم، امضای متد همچنان باید چیزی مثل این باشد:

<div dir="ltr">

```java
public void process(Object data) {
    // بررسی annotation در زمان اجرا
}
```
</div>

برای تشخیص اینکه شیء علامت‌گذاری شده یا نه، باید بررسی دیگری انجام دهیم؛ معمولاً با reflection:

<div dir="ltr">

```java
if (!data.getClass().isAnnotationPresent(SerializableMarker.class)) {
    throw new IllegalArgumentException("Unsupported type");
}
```
</div>

> **در این طراحی، خطا به‌جای زمان کامپایل، در زمان اجرا کشف می‌شود.**

<a id="serializable-note"></a>
### یک نکتهٔ ظریف دربارهٔ `Serializable` واقعی در Java

ممکن است بپرسی: اگر `Serializable` این‌قدر مفید است، چرا API استاندارد Java متد `ObjectOutputStream.writeObject` را این‌گونه تعریف کرده است؟

<div dir="ltr">

```java
public final void writeObject(Object obj) throws IOException
```
</div>

پارامتر آن `Object` است، نه `Serializable`. به همین دلیل، کد زیر از نظر کامپایل معتبر است:

<div dir="ltr">

```java
ObjectOutputStream out = /* ... */;
out.writeObject(new Object());  // ✅ Compile، ❌ Runtime NotSerializableException
```
</div>

اگر API به این صورت طراحی شده بود:

<div dir="ltr">

```java
public void writeObject(Serializable obj)
```
</div>

کامپایلر می‌توانست بسیاری از خطاها را زودتر تشخیص دهد.

> **درس طراحی:** اگر قرار است یک متد فقط اشیای دارای ویژگی مشخصی را بپذیرد، تعریف یک interface برای آن ویژگی می‌تواند امکان بررسی ایستا و امن‌تر را فراهم کند.

[بازگشت به بالا](#top)

---

<a id="type-restriction"></a>
## ۴. مزیت دوم: محدودکردن دقیق‌تر محل استفاده

مزیت دوم Marker Interface این است که می‌تواند محدودهٔ انواعی را که اجازه دارند آن را پیاده‌سازی کنند، محدود کند.

<div dir="ltr">

```java
interface Plugin {
    void execute();
}

interface TrustedPlugin extends Plugin {
}

class InternalPlugin implements TrustedPlugin {
    @Override
    public void execute() { }
}

class ExternalPlugin implements Plugin {
    @Override
    public void execute() { }
}
```
</div>

حالا `TrustedPlugin` هم یک علامت است و هم یک **نوع فرعی** از `Plugin`:

<div dir="ltr">

```java
void executeTrusted(TrustedPlugin plugin) {
    plugin.execute();
}

executeTrusted(new InternalPlugin());  // ✅
executeTrusted(new ExternalPlugin());  // ❌ Compile Error
```
</div>

<a id="annotation-difference"></a>
### همین کار با Annotation چه تفاوتی دارد؟

<div dir="ltr">

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Trusted {
}

@Trusted
class InternalPlugin implements Plugin { }
```
</div>

اما `@Trusted` تضمین نمی‌کند که کلاس حتماً `Plugin` را پیاده‌سازی کرده باشد. `@Target(ElementType.TYPE)` اجازه می‌دهد annotation روی کلاس‌ها و interfaceها قرار بگیرد، بدون اینکه رابطهٔ وراثت یا پیاده‌سازی خاصی را تحمیل کند.

| ویژگی | `TrustedPlugin extends Plugin` | `@Trusted` |
|--------|-------------------------------|------------|
| محدودیت در سیستم نوع | ✅ | ❌ |
| metadata | ✅ | ✅ |
| بررسی رابطهٔ نوعی | خودکار در کامپایل | نیازمند بررسی جداگانه |

[بازگشت به بالا](#top)

---

<a id="set-marker"></a>
## ۵. آیا `Set` هم یک Marker Interface است؟

نویسنده برای توضیح این موضوع به `Set` اشاره می‌کند:

<div dir="ltr">

```java
public interface Set<E> extends Collection<E> {
    // ...
}
```
</div>

اینترفیس `Set` متدهای جدیدی نسبت به `Collection` اضافه نمی‌کند، اما **نوع متفاوتی** را تعریف می‌کند.

<div dir="ltr">

```java
Collection<String> names = new ArrayList<>();
Set<String> uniqueNames = new HashSet<>();
```
</div>

هر `Set` یک `Collection` است، ولی هر `Collection` لزوماً یک `Set` نیست.

> **نکته:** `Set` صرفاً یک Marker Interface معمولی نیست؛ زیرا علاوه بر مشخص‌کردن نوع، **قرارداد معنایی** چند متد را هم دقیق‌تر می‌کند. برای نمونه، قرارداد `add` در `Set` با قرارداد آن در `Collection` تفاوت معنایی دارد: اضافه‌کردن عنصری که از قبل در مجموعه وجود دارد، نباید باعث ایجاد عضو تکراری شود.

نویسنده می‌گوید به‌راحتی می‌توان Marker Interface دیگری را تصور کرد که فقط برای زیرنوع‌های یک interface خاص قابل استفاده باشد:

<div dir="ltr">

```java
interface Cacheable {
}

interface DomainObject {
    String getId();
}

interface CacheableDomainObject
        extends DomainObject, Cacheable {
}
```
</div>

`CacheableDomainObject` نشان می‌دهد که یک نوع هم `DomainObject` است و هم ویژگی `Cacheable` را دارد؛ بدون آنکه متد جدیدی معرفی کند.

[بازگشت به بالا](#top)

---

<a id="annotation-advantage"></a>
## ۶. پس Marker Annotation چه مزیتی دارد؟

تا اینجا ممکن است به این نتیجه برسیم که Marker Interface همیشه انتخاب بهتری است؛ اما نویسنده چنین ادعایی نمی‌کند.

Marker Annotation هم مزیت مهمی دارد: بخشی از **سیستم یکپارچهٔ annotationهای Java** است و با frameworkهایی که بر پایهٔ annotation کار می‌کنند، هماهنگی طبیعی دارد.

برای مثال، فرض کن می‌خواهیم بعضی متدها را برای یک ابزار تست مشخص کنیم:

<div dir="ltr">

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface FastTest {
}

class UserServiceTest {
    @FastTest
    void shouldCreateUser() { }
}
```
</div>

> **نکتهٔ مهم:** تعریف یک Marker Interface نمی‌تواند جایگزین مستقیمی برای این نیاز باشد؛ چون متدها نمی‌توانند یک interface را پیاده‌سازی کنند.

| عنصری که علامت می‌گیرد | Marker Interface | Marker Annotation |
|------------------------|------------------|-------------------|
| کلاس‌ها و interfaceها | ✅ | ✅ |
| متدها | ❌ | ✅ |
| فیلدها | ❌ | ✅ |
| پارامترها | ❌ | ✅ |
| سایر عناصر مجاز طبق `@Target` | ❌ | ✅ |

[بازگشت به بالا](#top)

---

<a id="how-to-choose"></a>
## ۷. چطور بین Marker Interface و Marker Annotation انتخاب کنیم؟

نویسنده یک معیار تصمیم‌گیری بسیار کاربردی پیشنهاد می‌کند:

```
۱. علامت روی چه چیزی اعمال می‌شود؟
   └── فقط کلاس‌ها و interfaceها؟
       ├── بله → سؤال ۲
       └── خیر → Marker Annotation

۲. آیا به APIای نیاز دارم که فقط اشیای دارای این علامت را بپذیرد؟
   ├── بله → Marker Interface (type checking)
   └── خیر → Marker Annotation (metadata)
```

### راهنمای انتخاب

| وضعیت | انتخاب پیشنهادی |
|--------|------------------|
| علامت فقط روی کلاس‌ها/interfaceها + نیاز به type checking | Marker Interface |
| علامت روی متد/فیلد/پارامتر | Marker Annotation |
| علامت بخشی از framework مبتنی بر annotation | Marker Annotation |
| نیاز به محدودکردن زیرنوع‌ها با رابطهٔ نوعی | Marker Interface |

> **این راهنما یک قاعدهٔ عملی است، نه قانون مطلق؛ در طراحی واقعی باید قراردادهای API، سازگاری با کد موجود و نیازهای framework را هم در نظر گرفت.**

[بازگشت به بالا](#top)

---

<a id="connection-item22"></a>
## ۸. ارتباط Item 41 با Item 22

نویسنده در پایان می‌گوید این آیتم از یک جهت، عکس Item 22 است.

| Item | اصل |
|------|-----|
| **Item 22** | اگر نمی‌خواهی یک نوع تعریف کنی، از interface استفاده نکن |
| **Item 41** | اگر واقعاً می‌خواهی یک نوع تعریف کنی، از interface استفاده کن |

این دو توصیه با هم سازگارند. هر interface نباید صرفاً برای گروه‌بندی متدهای نامرتبط ساخته شود؛ اما از طرف دیگر، نباید interface را فقط به این دلیل کنار بگذاریم که هیچ متدی در آن وجود ندارد.

> **گاهی هدف اصلی یک interface، تعریف یک قابلیت یا دستهٔ نوعی است، نه تعریف رفتار جدید.**

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

### سه نکتهٔ اصلی Item 41

| # | نکته |
|---|------|
| **۱** | Marker Interface یک **نوع واقعی** تعریف می‌کند. بنابراین می‌توان از آن در امضای متدها استفاده کرد و از type checking در زمان کامپایل بهره برد |
| **۲** | Marker Annotation برای **metadata** مناسب است. به‌ویژه وقتی علامت باید روی عناصر مختلف برنامه قرار بگیرد یا بخشی از یک framework مبتنی بر annotation باشد |
| **۳** | خالی‌بودن یک interface به معنای بی‌فایده‌بودن آن نیست. اگر هدف تعریف یک نوع باشد، حتی interface بدون متد هم می‌تواند ارزش طراحی قابل‌توجهی داشته باشد |

### مقایسه نهایی

| معیار | Marker Interface | Marker Annotation |
|--------|------------------|-------------------|
| نوع جدید تعریف می‌کند | ✅ | ❌ |
| قابل استفاده به‌عنوان نوع پارامتر | ✅ | ❌ |
| type checking در زمان کامپایل | ✅ | ❌ |
| قابل اعمال روی متد/فیلد/پارامتر | ❌ | ✅ |
| مناسب frameworkهای annotation-based | ⚠️ | ✅ |
| هماهنگی با سیستم نوع Java | ✅ | ❌ |

### قاعدهٔ نهایی کتاب

> **اگر فقط به یک علامت نیاز داری، معمولاً annotation مناسب است؛ اما اگر می‌خواهی آن علامت بخشی از سیستم نوع‌های Java باشد، Marker Interface را جدی بگیر.**

---

[بازگشت به بالا](#top)

</div>