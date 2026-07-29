<div dir="rtl">

<a id="top"></a>

# آیتم ۱۵: دسترسی کلاس‌ها و اعضا را تا حد ممکن محدود کنید

## (Minimize the accessibility of classes and members)

---

## فهرست مطالب

- [بخش اول: Information Hiding؛ مهم‌ترین اصل طراحی نرم‌افزار](#part1)
  - [مقدمه](#introduction)
  - [چرا این جمله این‌قدر مهم است؟](#why-important)
  - [Information Hiding چیست؟](#what-is-information-hiding)
  - [API و Implementation](#api-vs-implementation)
  - [Encapsulation چیست؟](#encapsulation)
  - [چرا Information Hiding این‌قدر مهم است؟](#why-information-hiding)
  - [Java چگونه Information Hiding را پیاده‌سازی می‌کند؟](#java-mechanisms)
  - [قانون طلایی Joshua Bloch](#golden-rule)
  - [سطوح دسترسی برای Top-Level Class](#top-level-access)
  - [Public یعنی یک تعهد دائمی](#public-commitment)
  - [جمع‌بندی بخش اول](#part1-summary)

- [بخش دوم: اصل Private by Default، چهار سطح دسترسی، API Design و LSP](#part2)
  - [چهار سطح دسترسی اعضا (Members)](#four-levels)
  - [قانون شماره یک Bloch](#rule-one)
  - [Private یعنی آزادی کامل](#private-freedom)
  - [Package-Private](#package-private)
  - [Protected](#protected)
  - [Public](#public)
  - [ارتباط با Liskov Substitution Principle](#lsp)
  - [افزایش دسترسی برای Test؟](#test-access)
  - [Anti-Patternهای رایج](#anti-patterns)
  - [Best Practiceهای این بخش](#best-practices)
  - [جمع‌بندی بخش دوم](#part2-summary)

- [بخش سوم: Public Field، آرایه‌ها، Java Modules و جمع‌بندی نهایی](#part3)
  - [Public Field = از دست دادن کنترل روی کلاس](#public-field)
  - [Invariant چیست؟](#invariant)
  - [مشکل Validation](#validation)
  - [مشکل Side Effect](#side-effect)
  - [Thread Safety](#thread-safety)
  - [حتی اگر final باشد؟](#final-field)
  - [Public Static Final](#public-static-final)
  - [بزرگ‌ترین تله: Public Static Final Array](#array-trap)
  - [راه‌حل اول: Unmodifiable List](#solution1)
  - [راه‌حل دوم: Array Copy](#solution2)
  - [کدام بهتر است؟](#which-is-better)
  - [Java 9 Modules](#java-modules)
  - [جمع‌بندی نهایی Item 15](#final-summary)

[بازگشت به بالا](#top)

---

<a id="part1"></a>
## بخش اول: Information Hiding؛ مهم‌ترین اصل طراحی نرم‌افزار

<a id="introduction"></a>
### مقدمه

اگر از Joshua Bloch بپرسید:

> **مهم‌ترین ویژگی یک Component خوب چیست؟**

احتمالاً پاسخ او چیزی شبیه این خواهد بود:

> **اینکه دیگران هیچ اطلاعی از نحوه‌ی پیاده‌سازی داخلی آن نداشته باشند.**

این دقیقاً همان چیزی است که کتاب با جمله‌ی بسیار معروف زیر آغاز می‌کند:

> *The single most important factor that distinguishes a well-designed component from a poorly designed one is the degree to which the component hides its internal data and other implementation details.*

یعنی:

> **مهم‌ترین عاملی که یک کامپوننت خوب را از یک کامپوننت بد متمایز می‌کند، میزان پنهان کردن جزئیات پیاده‌سازی آن است.**

این جمله، یکی از مهم‌ترین اصول مهندسی نرم‌افزار است.

<a id="why-important"></a>
### چرا این جمله این‌قدر مهم است؟

فرض کنید دو تیم در حال توسعه یک سیستم بانکی هستند.

- تیم اول روی Payment Service کار می‌کند.
- تیم دوم روی Notification Service.

اگر تیم دوم مجبور باشد بداند Payment چگونه Transaction را ذخیره می‌کند، چه ORMای استفاده می‌کند، چه Cacheای دارد، چه دیتابیسی دارد و... این دو تیم به شدت به هم وابسته می‌شوند.

اما اگر تنها چیزی که بدانند این باشد:

<div dir="ltr">

```java
PaymentService.process(payment)
```
</div>

دیگر هیچ وابستگی داخلی وجود ندارد. این دقیقاً همان چیزی است که Information Hiding ایجاد می‌کند.

<a id="what-is-information-hiding"></a>
### Information Hiding چیست؟

Information Hiding یعنی:

> **تمام جزئیات داخلی کلاس از سایر قسمت‌های سیستم مخفی شوند و فقط یک API مشخص در اختیار کاربران کلاس قرار گیرد.**

در نتیجه کاربران فقط می‌دانند **چه کاری انجام می‌شود**، اما نمی‌دانند **چگونه انجام می‌شود**.

#### مثال

**بد:**

<div dir="ltr">

```java
public class PaymentService {
    public JdbcTemplate jdbc;
    public Cache cache;
    public Connection connection;
}
```
</div>

همه چیز بیرون قابل مشاهده است. در نتیجه تمام سیستم به Implementation وابسته می‌شود.

**خوب:**

<div dir="ltr">

```java
public class PaymentService {
    private final PaymentRepository repository;

    public PaymentResult process(Payment payment) {
        // ...
    }
}
```
</div>

کاربر فقط API را می‌بیند.

<a id="api-vs-implementation"></a>
### API و Implementation

Bloch جمله‌ی بسیار مهمی می‌گوید:

> A well-designed component hides all implementation details, cleanly separating its API from its implementation.

یعنی یک Component خوب باید بین **API** و **Implementation** یک مرز مشخص ایجاد کند.

#### معماری
<div dir="ltr">

```
                 Client
                    │
                    │
             Public API
                    │
────────────────────────────────
      Internal Implementation
────────────────────────────────
Repository
Cache
Connection Pool
Logger
Thread Pool
Private Methods
```
</div>
Client فقط قسمت بالایی را می‌بیند.

<a id="encapsulation"></a>
### Encapsulation چیست؟

Information Hiding معمولاً توسط Encapsulation پیاده‌سازی می‌شود. اما این دو دقیقاً یکی نیستند.

| Encapsulation | Information Hiding |
|---------------|-------------------|
| مکانیزم زبان | اصل طراحی |
| توسط `private` انجام می‌شود | هدف معماری است |
| ویژگی Java | ویژگی Software Design |

به عبارت دیگر:
<div dir="ltr">

```
Information Hiding
        │
        ▼
Encapsulation
        │
        ▼
private → package-private → protected → public
```
</div>
<a id="why-information-hiding"></a>
### چرا Information Hiding این‌قدر مهم است؟

Bloch پنج دلیل اصلی بیان می‌کند:

#### ۱. کاهش Coupling

فرض کنید `Order Service` مستقیماً به `MySQL` وابسته باشد:
<div dir="ltr">

```
Order → JDBC → MySQL
```
</div>
بعداً تصمیم می‌گیریم PostgreSQL استفاده کنیم. باید کل سیستم تغییر کند.

اما اگر فقط Repository دیده شود:
<div dir="ltr">

```
Order → Repository → Implementation
```
</div>
فقط یک کلاس تغییر می‌کند. این یعنی **Low Coupling**.

#### ۲. توسعه موازی

Bloch می‌گوید:
<div dir="ltr">

> Components can be developed in parallel.
</div>
فرض کنید ۵ تیم دارید:

- Authentication
- Payment
- Inventory
- Notification
- Reporting

اگر هر تیم فقط API تیم دیگر را بداند، همه می‌توانند همزمان توسعه دهند. بدون وابستگی.

#### ۳. نگهداری آسان‌تر

اگر Implementation مخفی باشد:
<div dir="ltr">

```
HashMap → ConcurrentHashMap
```
</div>
هیچ Clientای را خراب نمی‌کند.

اگر public باشد: کل سیستم وابسته می‌شود.

#### ۴. Performance Optimization

Bloch می‌گوید: Information Hiding خودش Performance را بهتر نمی‌کند. اما اجازه می‌دهد بعداً Performance را بهتر کنیم.

فرض کنید امروز `HashMap` دارید. بعداً Profiling نشان می‌دهد `ConcurrentHashMap` سریع‌تر است. اگر Implementation مخفی باشد، فقط یک کلاس تغییر می‌کند. اگر نباشد، کل پروژه باید بازنویسی شود.

#### ۵. Reusability

وقتی کلاس‌ها به هم وابسته نباشند، استفاده مجدد از آن‌ها بسیار آسان‌تر می‌شود.

مثلاً `JwtTokenGenerator` اگر فقط این API را داشته باشد:

<div dir="ltr">

```java
String generate(User user)
```
</div>

می‌تواند در Spring، Quarkus، Micronaut و Jakarta EE همگی استفاده شود.

#### ۶. کاهش ریسک پروژه‌های بزرگ

فرض کنید پروژه‌ای با ۳۰۰۰ کلاس دارید. اگر یکی از Moduleها شکست بخورد، ولی وابستگی کمی داشته باشد، کل پروژه نابود نمی‌شود.

به همین دلیل Bloch می‌گوید: Information Hiding ریسک پروژه‌های بزرگ را کاهش می‌دهد.

<a id="java-mechanisms"></a>
### Java چگونه Information Hiding را پیاده‌سازی می‌کند؟

جاوا از **Access Modifier** استفاده می‌کند:
<div dir="ltr">

```
private
package-private
protected
public
```
</div>
تمام Item 15 حول همین چهار کلمه می‌چرخد.

<a id="golden-rule"></a>
### قانون طلایی Joshua Bloch

این جمله را تقریباً باید حفظ کنید:
<div dir="ltr">

> **Make each class or member as inaccessible as possible.**
</div>
یعنی:

> **هر کلاس یا عضو را تا جایی که امکان دارد غیرقابل دسترس نگه دارید.**

نه کمتر، نه بیشتر.

این جمله را می‌توان به صورت زیر بازنویسی کرد:
<div dir="ltr">

```
Never make something public unless you really must.
```
</div>
#### اصل معماری

وقتی کلاسی طراحی می‌کنید، ترتیب تصمیم‌گیری باید این باشد:

```
private
    ↓
آیا کافی است؟
    ↓
اگر نه → package-private
    ↓
اگر نه → protected
    ↓
اگر نه → public
```

نه برعکس. بزرگ‌ترین اشتباه بسیاری از برنامه‌نویسان این است که از ابتدا همه چیز را `public` تعریف می‌کنند و بعداً تلاش می‌کنند آن را محدود کنند؛ در حالی که رویکرد صحیح دقیقاً برعکس است.

<a id="top-level-access"></a>
### سطوح دسترسی برای Top-Level Class

برای کلاس‌های معمولی (Non-Nested) فقط دو سطح دسترسی وجود دارد:

| Modifier | قابل دسترس از |
|----------|---------------|
| package-private | فقط داخل Package |
| public | همه جا |

دقت کنید: برای Top-Level Class، `private` وجود ندارد.

#### package-private چیست؟

اگر Modifier ننویسیم:

<div dir="ltr">

```java
class OrderValidator {
    // ...
}
```
</div>

کلاس به صورت **package-private** خواهد بود. فقط کلاس‌های همان Package آن را می‌بینند.

#### چرا Bloch این را دوست دارد؟

اگر این کلاس فقط داخل همان Package استفاده می‌شود، هیچ دلیلی ندارد Public باشد. زیرا:

```
Public → جزئی از API → تا ابد باید پشتیبانی شود
```

اما:

```
Package-private → Implementation → هر زمان بخواهیم حذف می‌شود
```

این یکی از مهم‌ترین تفاوت‌های بین **API عمومی** و **جزئیات پیاده‌سازی** است.

<a id="public-commitment"></a>
### Public یعنی یک تعهد دائمی

Bloch جمله بسیار مهمی دارد:
<div dir="ltr">

> **If you make it public, you are obligated to support it forever.**
</div>
یعنی: اگر امروز یک کلاس را Public کردی، در عمل به کاربران کتابخانه یا سیستم خود قول داده‌ای که در نسخه‌های آینده نیز از آن پشتیبانی کنی. به همین دلیل، Public کردن یک کلاس صرفاً یک تصمیم فنی نیست؛ بلکه یک **تصمیم طراحی API** است.

#### مثال معماری

فرض کنید در یک کتابخانه این کلاس را منتشر کرده‌اید:

<div dir="ltr">

```java
public class JsonParser {
    // ...
}
```
</div>

هزاران پروژه از آن استفاده می‌کنند.

یک سال بعد تصمیم می‌گیرید `JsonParser` را با `JacksonParser` جایگزین کنید. دیگر نمی‌توانید JsonParser را حذف کنید، زیرا هزاران پروژه به آن وابسته هستند.

اما اگر از ابتدا کلاس به صورت package-private بود، حذف یا بازنویسی آن هیچ تأثیری روی کاربران خارجی نداشت.

#### اگر فقط یک کلاس از آن استفاده می‌کند؟

Bloch یک توصیه بسیار جالب دارد. اگر یک Top-Level Class فقط توسط **یک کلاس دیگر** استفاده می‌شود، بهتر است آن را به یک `private static nested class` تبدیل کنید.

مثلاً به جای:

```text
OrderService
OrderValidator
```

می‌توان نوشت:

<div dir="ltr">

```java
public class OrderService {

    private static class OrderValidator {
        // ...
    }
}
```
</div>

در این حالت دامنه دسترسی از کل Package به **فقط یک کلاس** کاهش پیدا می‌کند. این دقیقاً همان چیزی است که اصل Information Hiding از ما می‌خواهد.

<a id="part1-summary"></a>
### جمع‌بندی بخش اول

در این بخش، پایه‌های فکری Item 15 را بررسی کردیم. پیام اصلی Joshua Bloch این است که طراحی خوب، از **پنهان‌سازی جزئیات پیاده‌سازی** آغاز می‌شود. هرچه وابستگی بین اجزای سیستم کمتر باشد، توسعه، نگهداری، تست، بهینه‌سازی و استفاده مجدد از کد ساده‌تر خواهد بود.

> **قانون طلایی این بخش: هر کلاس یا عضو را با کمترین سطح دسترسی ممکن طراحی کنید و فقط در صورت وجود یک نیاز واقعی، سطح دسترسی آن را افزایش دهید.**

[بازگشت به بالا](#top)

---

<a id="part2"></a>
## بخش دوم: اصل Private by Default، چهار سطح دسترسی، API Design و LSP

در بخش اول با فلسفه‌ی اصلی Item 15 آشنا شدیم. Joshua Bloch یک قانون طلایی مطرح کرد:
<div dir="ltr">

> **Make each class or member as inaccessible as possible.**
</div>
اما سؤال مهم اینجاست:
<div dir="ltr">

> **در عمل از کجا بفهمیم یک Member باید private باشد یا package-private یا protected یا public؟**
</div>
پاسخ این سؤال، تقریباً تمام بخش دوم Item 15 را تشکیل می‌دهد.

<a id="four-levels"></a>
### چهار سطح دسترسی اعضا (Members)

برخلاف Top-Level Classها که فقط دو سطح دسترسی دارند، اعضای کلاس (Members) چهار سطح دسترسی دارند.

Bloch آن‌ها را به ترتیب افزایش سطح دسترسی معرفی می‌کند:

| Modifier | دسترسی |
|----------|--------|
| `private` | فقط همان کلاس |
| `package-private` | تمام کلاس‌های همان Package |
| `protected` | همان Package + تمام Subclassها |
| `public` | همه جا |

به صورت معماری:

```
                    public
                       ▲
                       │
                 protected
                       ▲
                       │
              package-private
                       ▲
                       │
                   private
```

هرچه بالاتر برویم:
- تعداد کاربران بیشتر می‌شود
- وابستگی بیشتر می‌شود
- تغییر سخت‌تر می‌شود
- مسئولیت API بیشتر می‌شود

<a id="rule-one"></a>
### قانون شماره یک Bloch

بعد از طراحی Public API، تمام اعضا را `private` تعریف کنید.

Bloch می‌گوید:
<div dir="ltr">

> After carefully designing your class's public API, your reflex should be to make all other members private.
</div>
به عبارت دیگر: اول فکر نکنید `public`، بعداً اگر لازم شد `private`. بلکه دقیقاً برعکس.

#### تصمیم‌گیری صحیح

هر بار که می‌خواهید Member جدیدی اضافه کنید:

```
آیا فقط خود کلاس نیاز دارد؟
    ↓
بله → private
    ↓
نه → آیا فقط Package نیاز دارد؟
    ↓
بله → package-private
    ↓
نه → آیا Subclassها نیاز دارند؟
    ↓
بله → protected
    ↓
نه → public
```

این همان فرآیند تصمیم‌گیری است که یک Software Architect باید در ذهن داشته باشد.

<a id="private-freedom"></a>
### Private یعنی آزادی کامل

فرض کنید:

<div dir="ltr">

```java
public class OrderService {
    private Cache cache;
    private ConnectionPool pool;

    private void refreshCache() { }
}
```
</div>

شش ماه بعد تصمیم می‌گیرید `Cache` را با `Redis` جایگزین کنید. هیچ مشکلی وجود ندارد. چرا؟ زیرا هیچ Clientای این فیلدها را نمی‌بیند.

<a id="package-private"></a>
### Package-Private

گاهی یک کلاس دیگر داخل همان Package نیاز دارد به Member خاصی دسترسی داشته باشد. در این حالت Modifier را حذف می‌کنیم.

<div dir="ltr">

```java
class OrderValidator {
    void validate(Order order) {
        // ...
    }
}
```
</div>

این متد فقط داخل همان Package قابل مشاهده است.

#### چه زمانی Package-Private مناسب است؟

فرض کنید `order` Package شما شامل این کلاس‌هاست:
<div dir="ltr">

```text
order
├── OrderService
├── OrderValidator
├── OrderMapper
└── OrderRepository
```
</div>
تمام این کلاس‌ها متعلق به یک Domain هستند. اگر Validator فقط توسط همین Package استفاده شود، Public بودن آن اشتباه است.

#### اشتباه رایج

بعضی برنامه‌نویسان می‌گویند: "شاید بعداً نیاز شد." و همه چیز را Public می‌کنند:

<div dir="ltr">

```java
public class OrderValidator {
    public boolean validate(...) { }
}
```
</div>

در حالی که هیچ کلاس خارج از Package هرگز نباید Validator را ببیند.

#### نشانه طراحی بد

Bloch جمله جالبی دارد. اگر دائماً مجبور می‌شوید `private` → `package-private` تبدیل کنید، احتمالاً طراحی Package شما مشکل دارد. یعنی کلاس‌ها بیش از حد به هم وابسته‌اند.

**مثال بد:**
<div dir="ltr">

```text
service → repository → mapper → validator → util → converter
```
</div>
همه به هم وابسته‌اند.

**بهتر:**
<div dir="ltr">

```text
order
├── Service
├── Validator
└── Mapper

inventory
├── Service
├── Validator
└── Mapper
```
</div>
هر Package مستقل است.

#### Private vs Package-Private

| ویژگی | Private | Package-private |
|-------|---------|-----------------|
| فقط همان کلاس | ✅ | ❌ |
| همان Package | ❌ | ✅ |
| مناسب Internal Logic | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| کمترین Coupling | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |

در طراحی Enterprise، تا زمانی که واقعاً نیاز نباشد، Package-private هم زیاد استفاده نمی‌شود.

<a id="protected"></a>
### Protected

این قسمت یکی از مهم‌ترین قسمت‌های Item 15 است.

خیلی‌ها فکر می‌کنند:
<div dir="ltr">

```
protected → امن‌تر از public
```
</div>
اما Bloch دیدگاه دیگری دارد.

#### Protected یعنی چه؟

Member قابل دسترسی است از:
- تمام کلاس‌های همان Package
- **و** تمام Subclassها، حتی اگر Subclass در Package دیگری باشد.

معماری:
<div dir="ltr">

```
    Package A
        │
       Base
        ▲
        │
Subclass (Package B)
        ▲
        │
Subclass (Package C)
```
</div>
همه این Subclassها به Member دسترسی دارند.

#### مشکل Protected

Bloch می‌گوید:
<div dir="ltr">

> A protected member is part of the exported API.
</div>
این جمله فوق‌العاده مهم است. خیلی‌ها فکر می‌کنند Protected یعنی Internal. در حالی که نه. **Protected جزئی از API است.**

چرا؟ چون Subclassها روی آن برنامه می‌نویسند. اگر فردا حذفش کنید، تمام Subclassها خراب می‌شوند.

بنابراین Protected یعنی: **من تا سال‌ها از این Member پشتیبانی خواهم کرد.**

#### Protected یعنی تعهد به Implementation

Bloch جمله حتی مهم‌تری دارد:
<div dir="ltr">

> A protected member represents a public commitment to an implementation detail.
</div>
این جمله را باید چند بار خواند. وقتی چیزی Protected می‌شود، شما بخشی از نحوه پیاده‌سازی داخلی را به بیرون لو می‌دهید.

مثال:

<div dir="ltr">

```java
protected List<Order> cache;
```
</div>

تمام Subclassها فرض می‌کنند `cache` یک `List` است. اگر بعداً بخواهید `List` → `Map` کنید، تمام Subclassها خراب می‌شوند.

به همین دلیل: **Protected بسیار گران است.**

#### چه زمانی Protected؟

تقریباً فقط زمانی که:
- Framework طراحی می‌کنید
- مثلاً Spring، Hibernate، JUnit، Java Collections

که انتظار دارید کاربران از کلاس شما ارث‌بری کنند.

<a id="public"></a>
### Public

Public یعنی **تمام دنیا** می‌توانند از Member استفاده کنند. پس هر Member Public بخشی از قرارداد دائمی API شماست.

<a id="lsp"></a>
### ارتباط با Liskov Substitution Principle

Bloch در این بخش به یکی از مهم‌ترین اصول SOLID اشاره می‌کند.

اگر متدی را Override می‌کنید، حق ندارید سطح دسترسی را محدودتر کنید.

مثال:

<div dir="ltr">

```java
class Animal {
    public void move() { }
}
```
</div>

اشتباه:

<div dir="ltr">

```java
class Dog extends Animal {
    @Override
    protected void move() { }  // ❌ Cannot reduce visibility
}
```
</div>

Compiler: `Cannot reduce visibility`

چرا؟ چون اصل LSP می‌گوید هر جا بتوان از `Animal` استفاده کرد، باید بتوان `Dog` را نیز جایگزین کرد. اگر دسترسی کمتر شود: `Animal` → `move()` دارد، `Dog` → ندارد. اصل LSP شکسته می‌شود.

#### اگر Interface پیاده‌سازی کنیم چه؟

فرض کنید:

<div dir="ltr">

```java
public interface PaymentService {
    void process();
}
```
</div>

پیاده‌سازی:

<div dir="ltr">

```java
public class StripePaymentService implements PaymentService {
    @Override
    public void process() { }
}
```
</div>

متد `process()` حتماً باید `public` باشد. زیرا Interface یک قرارداد عمومی (Public Contract) است و پیاده‌سازی نمی‌تواند آن را محدودتر کند.

<a id="test-access"></a>
### افزایش دسترسی برای Test؟

یکی از اشتباهات رایج: "برای Unit Test → public کنیم."

Bloch می‌گوید: تا حدی قابل قبول است.

مثلاً `private` → `package-private` برای تست اشکالی ندارد. چون تست‌ها می‌توانند داخل همان Package اجرا شوند.

اما `private` → `public` فقط برای تست؟ **هرگز.**

چرا؟ چون دارید API را برای Test خراب می‌کنید. Test نباید طراحی API را تعیین کند.

<a id="anti-patterns"></a>
### Anti-Patternهای رایج

#### Anti-Pattern شماره ۱: Public به‌صورت پیش‌فرض

<div dir="ltr">

```java
public class UserService {
    public UserRepository repository;
    public UserValidator validator;
    public Cache cache;
}
```
</div>

مشکل:
- Coupling بسیار زیاد
- نقض Encapsulation
- تغییرناپذیری پایین
- تست دشوار
- نگهداری سخت

#### Anti-Pattern شماره ۲: Protected بدون نیاز

<div dir="ltr">

```java
protected List<User> cache;
```
</div>

در حالی که هیچ Subclass واقعی وجود ندارد. Protected فقط به امید "شاید روزی لازم شود" یک انتخاب طراحی اشتباه است.

#### Anti-Pattern شماره ۳: Packageهای به‌شدت وابسته

اگر تقریباً تمام کلاس‌های Package به تمام کلاس‌های دیگر دسترسی package-private نیاز دارند، معمولاً نشانه‌ی آن است که مرزهای Package به‌درستی طراحی نشده‌اند و باید ساختار سیستم بازنگری شود.

<a id="best-practices"></a>
### Best Practiceهای این بخش

- **Private by Default**: ابتدا همه چیز را `private` در نظر بگیرید.
- **Package-private برای همکاری داخلی**: فقط زمانی که چند کلاس در یک Package واقعاً نیاز به همکاری نزدیک دارند.
- **Protected فقط برای Extension Pointها**: زمانی که طراحی شما عمداً برای ارث‌بری و توسعه توسط دیگران ساخته شده است.
- **Public فقط برای API پایدار**: هر عضو Public یک قرارداد بلندمدت با کاربران سیستم است.

<a id="part2-summary"></a>
### جمع‌بندی بخش دوم

پیام اصلی Joshua Bloch در این بخش این است که **سطح دسترسی یک تصمیم صرفاً نحوی (Syntax) نیست؛ بلکه یک تصمیم معماری است**. هر بار که سطح دسترسی عضوی را افزایش می‌دهید، در واقع تعداد مصرف‌کنندگان آن، میزان Coupling، هزینه نگهداری و تعهد خود نسبت به سازگاری نسخه‌های آینده را نیز افزایش می‌دهید.

به همین دلیل، معماران نرم‌افزار معمولاً این اصل را دنبال می‌کنند:

> **هر عضو را با کمترین سطح دسترسی ممکن طراحی کن و تنها زمانی آن را افزایش بده که یک نیاز واقعی و اثبات‌شده وجود داشته باشد.**

[بازگشت به بالا](#top)

---

<a id="part3"></a>
## بخش سوم: Public Field، آرایه‌ها، Java Modules و جمع‌بندی نهایی

اگر دو بخش قبلی بیشتر درباره‌ی **Access Modifierها** و **Information Hiding** بودند، این بخش درباره‌ی **طراحی APIهای Public** است؛ جایی که Bloch یکی از مهم‌ترین قوانین طراحی کتابخانه‌های جاوا را مطرح می‌کند.

<a id="public-field"></a>
### Public Field = از دست دادن کنترل روی کلاس

Joshua Bloch تقریباً همیشه با Public Field مخالف است.

شاید در نگاه اول چیزی شبیه این کاملاً طبیعی به نظر برسد:

<div dir="ltr">

```java
public class Employee {
    public String name;
    public int age;
}
```
</div>

خیلی ساده است. هیچ Getter و Setter هم ندارد.

اما این طراحی تقریباً همیشه اشتباه است. چرا؟ چون از همان لحظه‌ای که Field را Public کردی، دیگر هیچ کنترلی روی Object نداری.

#### اصل مهم

کلاس باید تنها موجودیتی باشد که بتواند State خودش را تغییر دهد. اگر هر کسی بتواند مستقیماً Field را تغییر دهد، دیگر Class هیچ کنترلی روی وضعیت خودش ندارد.

مثال:

<div dir="ltr">

```java
public class BankAccount {
    public BigDecimal balance;
}
```
</div>

حالا هر جایی از برنامه:

<div dir="ltr">

```java
account.balance = new BigDecimal("-1000000");
```
</div>

تمام. Invariant کلاس نابود شد. کلاس حتی فرصت ندارد بررسی کند که موجودی منفی نشود.

در حالی که اگر بنویسیم:

<div dir="ltr">

```java
private BigDecimal balance;

public void withdraw(BigDecimal amount) {
    if (balance.compareTo(amount) < 0) {
        throw new IllegalStateException();
    }
    // ...
}
```
</div>

کلاس همیشه سالم می‌ماند.

<a id="invariant"></a>
### Invariant چیست؟

Invariant یعنی قوانینی که همیشه باید برقرار باشند. مثلاً:

| کلاس | Invariant |
|------|-----------|
| `Person` | `age >= 0` |
| `BankAccount` | `balance >= 0` |
| `Rectangle` | `width > 0, height > 0` |
| `Order` | `totalPrice >= 0` |

اگر Field عمومی باشد، هیچ تضمینی برای حفظ این قوانین وجود ندارد.

<a id="validation"></a>
### مشکل Validation

فرض کنید:

<div dir="ltr">

```java
public class User {
    public String email;
}
```
</div>

هر کسی می‌تواند بنویسد:

<div dir="ltr">

```java
user.email = "abc";
user.email = "####";
user.email = null;
```
</div>

اما اگر Setter داشته باشیم:

<div dir="ltr">

```java
public void changeEmail(String email) {
    Objects.requireNonNull(email);
    if (!EMAIL_PATTERN.matcher(email).matches()) {
        throw new IllegalArgumentException();
    }
    // ...
}
```
</div>

می‌توانیم اعتبارسنجی کنیم.

<a id="side-effect"></a>
### مشکل Side Effect

گاهی تغییر یک Field باید باعث انجام کارهای دیگری شود. مثلاً اگر `price` تغییر کند، باید `tax`، `discount`، `invoice` و `cache` هم آپدیت شوند.

اگر Field Public باشد، این اتفاق هرگز نمی‌افتد.

<a id="thread-safety"></a>
### Thread Safety

Bloch یک نکته بسیار مهم می‌گوید. اگر Field هم Public باشد هم Mutable، کلاس تقریباً هیچ وقت Thread Safe نیست.

مثال:

<div dir="ltr">

```java
public class Counter {
    public int count;
}
```
</div>

دو Thread: `count++` → Race Condition ایجاد می‌کنند.

در حالی که:

<div dir="ltr">

```java
private final AtomicInteger count;
```
</div>

یا `synchronized` کنترل کامل در اختیار کلاس است.

<a id="final-field"></a>
### حتی اگر final باشد؟

Bloch می‌گوید حتی اگر Field `public final` باشد باز هم ممکن است اشتباه باشد.

مثال:

<div dir="ltr">

```java
public final Address address;
```
</div>

خود Reference تغییر نمی‌کند. اما:

<div dir="ltr">

```java
address.setCity(...)
```
</div>

هنوز ممکن است. یعنی: Reference Immutable، Object Mutable. پس این هم خطرناک است.

<a id="public-static-final"></a>
### Public Static Final

اینجا تنها استثناست. Bloch می‌گوید اگر چیزی واقعاً Constant است، می‌تواند `public static final` باشد.

مثل:

<div dir="ltr">

```java
public static final double PI = 3.14159;
public static final int MAX_SIZE = 1024;
```
</div>

اما شرط دارد: آن شیء باید **Immutable** باشد.

**درست:**

<div dir="ltr">

```java
public static final String VERSION = "1.0";
public static final Duration TIMEOUT = Duration.ofSeconds(10);
```
</div>

چون `String` و `Duration` Immutable هستند.

**اشتباه:**

<div dir="ltr">

```java
public static final ArrayList<String> VALUES = new ArrayList<>();
```
</div>

چون:

<div dir="ltr">

```java
VALUES.add(...)  // کاملاً مجاز است
```
</div>

یعنی: Reference ثابت است، ولی Object تغییر می‌کند.

<a id="array-trap"></a>
### بزرگ‌ترین تله: Public Static Final Array

Bloch این قسمت را با هشدار جدی مطرح می‌کند.

مثلاً:

<div dir="ltr">

```java
public static final Thing[] VALUES = { ... };
```
</div>

خیلی‌ها فکر می‌کنند چون `final` است، امن است. اما:

<div dir="ltr">

```java
VALUES[0] = anotherThing;  // کاملاً مجاز است
```
</div>

پس: `final` ≠ `Immutable`

#### مثال

<div dir="ltr">

```java
public class Config {
    public static final String[] ROLES = {
        "ADMIN",
        "USER"
    };
}
```
</div>

جایی دیگر:

<div dir="ltr">

```java
Config.ROLES[0] = "ROOT";
```
</div>

تمام برنامه خراب شد. این یکی از مشهورترین Security Holeهای جاوا است.

<a id="solution1"></a>
### راه‌حل اول: Unmodifiable List

<div dir="ltr">

```java
private static final Thing[] PRIVATE_VALUES = { ... };

public static final List<Thing> VALUES =
        Collections.unmodifiableList(Arrays.asList(PRIVATE_VALUES));
```
</div>

**مزایا:**
- ✔ Immutable API
- ✔ ساده
- ✔ بدون Copy
- ✔ سریع

اما اگر خود Thing Mutable باشد، باز هم باید مراقب بود.

<a id="solution2"></a>
### راه‌حل دوم: Array Copy

هر بار یک Copy برگردان:

<div dir="ltr">

```java
private static final Thing[] PRIVATE_VALUES = { ... };

public static Thing[] values() {
    return PRIVATE_VALUES.clone();
}
```
</div>

هر بار یک Array جدید ساخته می‌شود.

**مزایا:** هیچ کس نمی‌تواند Array اصلی را خراب کند.

**عیب:** Allocation بیشتر.

<a id="which-is-better"></a>
### کدام بهتر است؟

Bloch می‌گوید به نیاز Client بستگی دارد:

| نیاز | راه‌حل |
|------|--------|
| Read Only API | `Unmodifiable List` |
| Compatibility با APIهای قدیمی | Array Copy |
| Performance بالا | Immutable List |
| امنیت | Immutable List |
| Interop با Native API | Array Copy |

<a id="java-modules"></a>
### Java 9 Modules

در انتهای Item، Bloch درباره Moduleها صحبت می‌کند.

قبل از Java 9 فقط این سطوح وجود داشت:
<div dir="ltr">

```
private → package → protected → public
```
</div>
بعد از Java 9، Module هم اضافه شد:
<div dir="ltr">

```
Module → Package → Class → Member
```
</div>
مثلاً `com.company.payment` را Export نمی‌کنیم. در نتیجه حتی اگر کلاس `public` باشد، بیرون Module دیده نمی‌شود.

یعنی Access Control یک لایه دیگر پیدا کرده است.

اما Bloch هشدار می‌دهد: فعلاً Moduleها را فقط وقتی استفاده کنید که واقعاً نیاز دارید. چون:
- وابستگی‌ها باید صریح تعریف شوند
- ساختار پروژه باید بازآرایی شود
- استفاده از آن‌ها خارج از JDK هنوز همه‌گیر نیست

<a id="final-summary"></a>
### جمع‌بندی نهایی Item 15

Joshua Bloch در این آیتم یک اصل بنیادین طراحی نرم‌افزار را بیان می‌کند:

> **هر کلاس، متد و فیلد را تا حد امکان غیرقابل‌دسترس (Least Accessible) نگه دارید و فقط در صورت وجود یک نیاز واقعی، سطح دسترسی آن را افزایش دهید.**

در عمل، این اصل به چند قانون ساده تبدیل می‌شود:

| قانون | توضیح |
|-------|-------|
| **Public API را کوچک نگه دارید** | هرچه سطح دسترسی بیشتری بدهید، تعهد بیشتری برای پشتیبانی دارید |
| **پیاده‌سازی را پنهان کنید** | از `private` و `package-private` برای جزئیات داخلی استفاده کنید |
| **Protected را محدود کنید** | فقط زمانی که واقعاً برای Extension Pointها لازم است |
| **از Fieldهای Public اجتناب کنید** | به‌ویژه اگر Mutable باشند |
| **ثابت‌ها را Immutable کنید** | فقط `public static final` با انواع Immutable |
| **آرایه‌های Public ندهید** | از `Collections.unmodifiableList` یا کپی استفاده کنید |
| **در نظر بگیرید که تغییرات داخلی نباید API را بشکنند** | Encapsulation پایه‌ی Maintainability است |

#### توصیه Production-Grade

در پروژه‌های سازمانی مبتنی بر **Spring Boot، Quarkus، Micronaut** یا معماری **Microservices**، معمولاً این الگو رعایت می‌شود:

<div dir="ltr">

```java
public final class OrderService {

    private final OrderRepository repository;
    private final PaymentGateway gateway;

    public OrderService(OrderRepository repository, PaymentGateway gateway) {
        this.repository = repository;
        this.gateway = gateway;
    }

    public Order placeOrder(CreateOrderRequest request) {
        // Business Logic
    }

    // تمام Helperها private هستند
    private void validate(CreateOrderRequest request) {
        // ...
    }

    private Order mapToOrder(CreateOrderRequest request) {
        // ...
    }
}
```
</div>

در این طراحی:
- فقط عملیاتی که بخشی از **API سرویس** هستند `public` می‌شوند
- تمام جزئیات پیاده‌سازی (`validate`، متدهای کمکی، وضعیت داخلی و وابستگی‌ها) مخفی می‌مانند
- تغییرات داخلی بدون شکستن قرارداد عمومی امکان‌پذیر است؛ دقیقاً همان هدفی که Bloch از **Information Hiding** دنبال می‌کند

---

[بازگشت به بالا](#top)

</div>
```