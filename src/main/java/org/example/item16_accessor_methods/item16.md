<div dir="rtl">

<a id="top"></a>

# آیتم ۱۶: در کلاس‌های Public از متدهای دسترسی استفاده کنید، نه فیلدهای Public

## (In Public Classes, Use Accessor Methods, Not Public Fields)

---

## فهرست مطالب

- [مقدمه](#introduction)
- [چرا Public Field مشکل‌ساز است؟](#why-public-field)
  - [مشکل اول: از دست دادن قابلیت تغییر Implementation](#problem1)
  - [مشکل دوم: عدم امکان Validation](#problem2)
  - [مشکل سوم: عدم امکان Side Effect](#problem3)
- [راه‌حل پیشنهادی Bloch](#solution)
- [آیا همیشه Getter و Setter لازم است؟](#always-getter-setter)
- [کلاس‌های Package-Private یک استثنا هستند](#package-private)
- [تفاوت Public API و Internal Model](#api-vs-internal)
- [چرا در پروژه‌های Enterprise این موضوع مهم‌تر است؟](#enterprise-importance)
- [کلاس‌های معروفی که این قانون را نقض کرده‌اند](#famous-violations)
- [آیا Public Final Field قابل قبول است؟](#public-final)
- [مقایسه طراحی‌ها](#comparison)
- [Best Practice در Java مدرن](#modern-best-practice)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="introduction"></a>
## مقدمه

Joshua Bloch در ابتدای این آیتم یک مثال بسیار ساده ارائه می‌دهد:

<div dir="ltr">

```java
class Point {
    public double x;
    public double y;
}
```
</div>

در نگاه اول این کلاس کاملاً طبیعی است.

- دو فیلد دارد.
- هیچ منطق خاصی ندارد.
- استفاده از آن بسیار راحت است.

<div dir="ltr">

```java
Point point = new Point();
point.x = 10;
point.y = 20;
```
</div>

اما Bloch می‌گوید این نوع کلاس‌ها، که تنها مجموعه‌ای از فیلدهای Public هستند، برای کلاس‌های Public تقریباً همیشه یک **Design Smell** محسوب می‌شوند.

[بازگشت به بالا](#top)

---

<a id="why-public-field"></a>
## چرا Public Field مشکل‌ساز است؟

مشکل اصلی این است که با Public کردن فیلدها، **Encapsulation** از بین می‌رود.

در Item 15 یاد گرفتیم که هدف Encapsulation این است که:

> کلاس تنها موجودیتی باشد که بتواند وضعیت داخلی (State) خودش را کنترل کند.

اما در اینجا:

```
    Client
      │
      ▼
Public Field
      │
      ▼
Object State
```

هر کدی می‌تواند مستقیماً State را تغییر دهد.

<a id="problem1"></a>
### مشکل اول: از دست دادن قابلیت تغییر Implementation

فرض کنید نسخه اول کلاس به این شکل است:

<div dir="ltr">

```java
public class Point {
    public double x;
    public double y;
}
```
</div>

تمام Clientها از این API استفاده می‌کنند:

<div dir="ltr">

```java
point.x
```
</div>

حال فرض کنید بعداً بخواهیم مختصات را به صورت Polar ذخیره کنیم. به جای `x` و `y`، داخلی کلاس شود:
<div dir="ltr">

```
radius
angle
```
</div>
اما API قبلی `point.x` دیگر وجود ندارد.

یعنی: **Implementation تبدیل به API شده است.** و دیگر قابل تغییر نیست.

<a id="problem2"></a>
### مشکل دوم: عدم امکان Validation

فرض کنید:

<div dir="ltr">

```java
public class User {
    public int age;
}
```
</div>

اکنون:

<div dir="ltr">

```java
user.age = -50;  // کاملاً مجاز است
```
</div>

اما اگر Getter و Setter داشته باشیم:

<div dir="ltr">

```java
public void setAge(int age) {
    if (age < 0) {
        throw new IllegalArgumentException("Age cannot be negative");
    }
    this.age = age;
}
```
</div>

در نتیجه Invariant کلاس همیشه حفظ می‌شود.

<a id="problem3"></a>
### مشکل سوم: عدم امکان Side Effect

فرض کنید:

<div dir="ltr">

```java
public class Product {
    public BigDecimal price;
}
```
</div>

اما تغییر قیمت باید باعث شود:

- Cache پاک شود
- Event منتشر شود
- Log ثبت شود
- Discount دوباره محاسبه شود

اگر Field مستقیماً تغییر کند:

<div dir="ltr">

```java
product.price = newPrice;
```
</div>

هیچ‌کدام از این عملیات انجام نمی‌شوند.

اما اگر Setter داشته باشیم:

<div dir="ltr">

```java
public void setPrice(BigDecimal price) {
    this.price = price;
    clearCache();
    publishPriceChangedEvent();
    logPriceUpdate();
    recalculateDiscount();
}
```
</div>

می‌توانیم تمام این رفتارها را در یک نقطه متمرکز کنیم.

[بازگشت به بالا](#top)

---

<a id="solution"></a>
## راه‌حل پیشنهادی Bloch

به جای Public Field:

<div dir="ltr">

```java
class Point {
    private double x;
    private double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }
}
```
</div>

اکنون:

```
    Client
      │
      ▼
Getter / Setter
      │
      ▼
Private Field
```

تمام کنترل در اختیار کلاس است.

[بازگشت به بالا](#top)

---

<a id="always-getter-setter"></a>
## آیا همیشه Getter و Setter لازم است؟

**خیر.**

این دقیقاً جایی است که بسیاری از برنامه‌نویسان برداشت اشتباهی از این آیتم دارند.

Bloch نمی‌گوید:

> همیشه Getter و Setter بنویس.

بلکه می‌گوید:

> **برای کلاس‌های Public از Accessor استفاده کن، نه Public Field.**

این دو جمله تفاوت بسیار بزرگی دارند.

[بازگشت به بالا](#top)

---

<a id="package-private"></a>
## کلاس‌های Package-Private یک استثنا هستند

Bloch می‌گوید اگر کلاس:

- `package-private` باشد
- یا `private nested class` باشد

گاهی Public Field کاملاً قابل قبول است.

چرا؟ چون این کلاس بخشی از API عمومی نیست. فقط داخل همان Package استفاده می‌شود.

### مثال

<div dir="ltr">

```java
class Point {
    double x;
    double y;
}
```
</div>

اگر این کلاس فقط داخل یک Package استفاده شود، هیچ مشکلی ندارد.

چرا؟ زیرا اگر فردا تصمیم بگیریم Representation را عوض کنیم، تمام استفاده‌کنندگان در همان Package هستند. اصلاح آن‌ها بسیار ساده است.

[بازگشت به بالا](#top)

---

<a id="api-vs-internal"></a>
## تفاوت Public API و Internal Model

این تفاوت را همیشه در ذهن داشته باشید.

```
Internal Class
      ↓
Implementation Detail
      ↓
آزادانه قابل تغییر
```

اما

```
Public Class
      ↓
Public API
      ↓
باید سال‌ها حفظ شود
```

[بازگشت به بالا](#top)

---

<a id="enterprise-importance"></a>
## چرا در پروژه‌های Enterprise این موضوع مهم‌تر است؟

فرض کنید یک Library نوشته‌اید. هزاران نفر از آن استفاده می‌کنند.

اگر امروز:

<div dir="ltr">

```java
public String name;
```
</div>

را حذف کنید، تمام کاربران Library با خطای Compile مواجه می‌شوند.

اما اگر Getter داشته باشید:

<div dir="ltr">

```java
public String getName()
```
</div>

می‌توانید Implementation را تغییر دهید بدون اینکه API تغییر کند.

[بازگشت به بالا](#top)

---

<a id="famous-violations"></a>
## کلاس‌های معروفی که این قانون را نقض کرده‌اند

Bloch مثال می‌زند:

- `java.awt.Point`
- `java.awt.Dimension`

این کلاس‌ها Public Field دارند. اما می‌گوید:

> این‌ها الگو نیستند. بلکه نمونه‌هایی هستند که نباید تکرار شوند.

حتی اشاره می‌کند که طراحی کلاس **Dimension** بعدها مشکلات Performance و محدودیت‌های تکامل API ایجاد کرد و این تصمیم همچنان در جاوا باقی مانده است.

[بازگشت به بالا](#top)

---

<a id="public-final"></a>
## آیا Public Final Field قابل قبول است؟

اینجا بحث کمی ظریف‌تر می‌شود.

فرض کنید:

<div dir="ltr">

```java
public final class Time {
    public final int hour;
    public final int minute;

    public Time(int hour, int minute) {
        if (hour < 0 || hour > 23)
            throw new IllegalArgumentException("Invalid hour");
        if (minute < 0 || minute > 59)
            throw new IllegalArgumentException("Invalid minute");
        this.hour = hour;
        this.minute = minute;
    }
}
```
</div>

اینجا `hour` و `minute` دیگر قابل تغییر نیستند.

پس آیا مشکلی وجود دارد؟

Bloch می‌گوید: **کمتر مضر است، اما هنوز ایده‌آل نیست.**

### چرا؟

زیرا هنوز Representation بخشی از API شده است. مثلاً امروز `hour` و `minute` داریم. فردا شاید بخواهیم فقط `LocalTime` داشته باشیم. اما دیگر نمی‌توانیم.

### مزیت Public Final

البته یک مزیت مهم دارد. Constructor می‌تواند اعتبار داده‌ها را بررسی کند. بعد از ساخته شدن Object، دیگر کسی نمی‌تواند آن را خراب کند.

[بازگشت به بالا](#top)

---

<a id="comparison"></a>
## مقایسه طراحی‌ها

| طراحی | مناسب برای Public API؟ | مزایا | معایب |
|-------|----------------------|-------|-------|
| `public` Mutable Field | ❌ | ساده | شکستن Encapsulation، عدم Validation، عدم حفظ Invariant |
| `public final` Immutable Field | ⚠️ | ساده، Immutable | Representation بخشی از API می‌شود |
| `private` Field + Getter/Setter | ✅ | انعطاف‌پذیر، قابل توسعه، قابل اعتبارسنجی | کمی Boilerplate |
| `private` Field + فقط Getter (Immutable) | ⭐ **بهترین انتخاب** | Encapsulation + Immutable + API پایدار | تقریباً بدون عیب |

[بازگشت به بالا](#top)

---

<a id="modern-best-practice"></a>
## Best Practice در Java مدرن

در پروژه‌های **Spring Boot، Quarkus** و معماری‌های سازمانی، الگوی رایج چنین است:

<div dir="ltr">

```java
public final class User {

    private final UserId id;
    private final String name;
    private final Email email;

    public User(UserId id, String name, Email email) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.email = Objects.requireNonNull(email);
    }

    public UserId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }
}
```
</div>

در این طراحی:

- State داخلی کاملاً مخفی است
- کلاس Immutable است
- Validation فقط در Constructor انجام می‌شود
- در آینده می‌توان Representation داخلی را بدون شکستن API تغییر داد

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

قانون اصلی این آیتم را می‌توان این‌گونه خلاصه کرد:

> **اگر کلاسی بخشی از API عمومی است، هرگز وضعیت داخلی آن را با Public Field در اختیار کاربران قرار ندهید. از Accessor Methodها (و در صورت نیاز Mutatorها) برای حفظ Encapsulation، اعتبارسنجی، حفظ Invariantها و امکان تکامل API استفاده کنید.**

| قانون | توضیح |
|-------|-------|
| **کلاس‌های Public** | هرگز از Public Field استفاده نکنید |
| **کلاس‌های Package-Private** | استفاده از Field مستقیم قابل قبول است |
| **کلاس‌های Immutable** | اگر Public Field با `final` باشند، کمتر مضرند، اما همچنان ایده‌آل نیستند |
| **کلاس‌های Mutable** | همیشه از Getter و Setter استفاده کنید |
| **تغییر Implementation** | با Accessorها، تغییر داخلی بدون شکستن API ممکن است |

### نکته کلیدی

در مقابل، برای **کلاس‌های package-private یا private nested** که صرفاً جزئیات پیاده‌سازی داخلی هستند، استفاده از Fieldهای مستقیم در صورت ساده‌تر شدن کد می‌تواند کاملاً منطقی و قابل قبول باشد. این یکی از معدود مواردی است که Joshua Bloch بین **API عمومی** و **پیاده‌سازی داخلی** تمایز روشنی قائل می‌شود.

---

[بازگشت به بالا](#top)

</div>
```