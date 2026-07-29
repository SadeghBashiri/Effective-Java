<div dir="rtl">

<a id="top"></a>

# آیتم ۲۱: Interfaceها را برای آینده طراحی کنید

## (Design Interfaces for Posterity)

به یکی از مهم‌ترین آیتم‌های کل کتاب رسیدیم. اگر بخواهم فقط چند Item از Effective Java را برای طراحی APIهای Enterprise انتخاب کنم، **Item 15، 17، 18، 20 و 21** قطعاً داخل لیست هستند.

Item 21 در واقع ادامه‌ی منطقی Item 20 است:

- Item 20 می‌گفت **Interface بهترین ابزار برای تعریف Type است.**
- Item 21 می‌گوید **اگر Interface منتشر کردی (Public API)، تقریباً برای همیشه با آن زندگی خواهی کرد.**

---

## فهرست مطالب

- [ایده اصلی](#core-idea)
- [قبل از Java 8 چه اتفاقی می‌افتاد؟](#pre-java8)
- [Default Method](#default-method)
- [چرا Default Method خطرناک است؟](#why-dangerous)
- [مثال واقعی کتاب: SynchronizedCollection](#real-example)
- [removeIf چگونه کار می‌کند؟](#removeif)
- [مهم‌ترین پیام کتاب](#main-message)
- [Default Method چه زمانی مناسب است؟](#when-default)
- [Default Method چه زمانی بد است؟](#when-bad)
- [قانون مهم](#golden-rule)
- [Default Method برای کمک به پیاده‌سازی‌های جدید](#for-new-implementations)
- [آیا می‌توان Method را حذف کرد؟](#can-remove)
- [آیا می‌توان Signature را تغییر داد؟](#can-change)
- [Interface تقریباً Immutable است](#immutable-interface)
- [چگونه Interface طراحی کنیم؟](#how-to-design)
- [ارتباط با Itemهای قبلی](#connection)
- [Best Practices](#best-practices)
- [Anti-Patternها](#anti-patterns)
- [جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="core-idea"></a>
## ایده اصلی

> **Interfaceها را طوری طراحی کن که انگار قرار است ده سال آینده نیز استفاده شوند.**

این جمله‌ی کلیدی Joshua Bloch در ابتدای این آیتم، تمام پیام Item 21 را خلاصه می‌کند. وقتی یک Interface را به‌عنوان بخشی از API عمومی منتشر می‌کنید، در واقع با کاربران خود یک **قرارداد بلندمدت** امضا کرده‌اید که تغییر آن در آینده تقریباً غیرممکن است.

[بازگشت به بالا](#top)

---

<a id="pre-java8"></a>
## قبل از Java 8 چه اتفاقی می‌افتاد؟

قبل از Java 8، اضافه کردن حتی یک متد به Interface تقریباً غیرممکن بود.

مثلاً:

<div dir="ltr">

```java
public interface PaymentProcessor {
    void pay(Payment payment);
}
```
</div>

ده‌ها شرکت این Interface را پیاده‌سازی کرده‌اند.

حالا شما تصمیم می‌گیرید:

<div dir="ltr">

```java
public interface PaymentProcessor {
    void pay(Payment payment);
    void refund(Payment payment);  // ❌ همه Implementationها خراب می‌شوند
}
```
</div>

نتیجه؟ تمام Implementationها دیگر Compile نمی‌شوند.

به همین دلیل Java 8 آمد و گفت: "بیایید Default Method اضافه کنیم."

[بازگشت به بالا](#top)

---

<a id="default-method"></a>
## Default Method

<div dir="ltr">

```java
public interface PaymentProcessor {
    void pay(Payment payment);

    default void refund(Payment payment) {
        throw new UnsupportedOperationException();
    }
}
```
</div>

اکنون همه Implementationهای قبلی بدون تغییر Compile می‌شوند.

به ظاهر همه چیز عالی است... اما کتاب می‌گوید:

> نه، هنوز خطر وجود دارد.

[بازگشت به بالا](#top)

---

<a id="why-dangerous"></a>
## چرا Default Method خطرناک است؟

چون Default Method بدون اطلاع نویسنده‌ی Implementation وارد کلاس او می‌شود.

فرض کنید کسی پنج سال قبل چنین کلاسی نوشته است:

<div dir="ltr">

```java
public class SecureCollection<E> implements Collection<E> {
    private final Object lock = new Object();

    public boolean add(E e) {
        synchronized(lock) {
            // ...
        }
    }
}
```
</div>

تمام متدها Synchronize شده‌اند. اما Java 8 این متد را اضافه می‌کند:

<div dir="ltr">

```java
default boolean removeIf(Predicate<? super E> filter) {
    // ...
}
```
</div>

که داخل Interface نوشته شده است. Implementation بالا آن را Override نکرده است.

در نتیجه `removeIf()` بدون Synchronization اجرا می‌شود. در نتیجه **Race Condition** یا **ConcurrentModificationException**.

یعنی هیچ Compile Error وجود ندارد. ولی Runtime خراب می‌شود. این بدترین نوع Bug است.

[بازگشت به بالا](#top)

---

<a id="real-example"></a>
## مثال واقعی کتاب

کتاب دقیقاً همین مثال را از **Apache Commons Collections** می‌آورد.

کلاس `SynchronizedCollection` قبل از Java 8 نوشته شده بود. وقتی `removeIf` اضافه شد، Implementation پیش‌فرض Thread Safe نبود. در نتیجه کل کلاس رفتار اشتباه پیدا کرد.

[بازگشت به بالا](#top)

---

<a id="removeif"></a>
## removeIf چگونه کار می‌کند؟

کتاب تقریباً چنین کدی را نشان می‌دهد:

<div dir="ltr">

```java
default boolean removeIf(Predicate<? super E> filter) {
    Iterator<E> it = iterator();
    while (it.hasNext()) {
        if (filter.test(it.next())) {
            it.remove();
        }
    }
    return true;
}
```
</div>

ظاهرش کاملاً منطقی است. اما یک فرض بزرگ دارد. فرض کرده است که `iterator()` کاملاً مستقل است. درحالی‌که بعضی Collectionها نیاز دارند `iterator()` داخل Lock اجرا شود.

[بازگشت به بالا](#top)

---

<a id="main-message"></a>
## مهم‌ترین پیام کتاب

> نوشتن یک Default Method عمومی که روی تمام Implementationهای دنیا درست کار کند تقریباً غیرممکن است.

### چرا؟

چون Interface فقط Contract را می‌شناسد. Implementation را نمی‌شناسد. ممکن است یکی:

- `Concurrent` باشد
- `Lazy` باشد
- `Database-backed` باشد
- `Distributed` باشد
- `Immutable` باشد
- `Remote` باشد

یک Default Method باید روی همه‌ی این‌ها درست کار کند. تقریباً غیرممکن است.

[بازگشت به بالا](#top)

---

<a id="when-default"></a>
## Default Method چه زمانی مناسب است؟

وقتی رفتار کاملاً عمومی باشد.

مثلاً:

<div dir="ltr">

```java
interface Shape {
    double area();

    default boolean isEmpty() {
        return area() == 0;
    }
}
```
</div>

عالی است. چرا؟ چون `isEmpty()` هیچ فرض اضافه‌ای درباره Implementation ندارد.

مثال دیگر:

<div dir="ltr">

```java
interface Person {
    String firstName();
    String lastName();

    default String fullName() {
        return firstName() + " " + lastName();
    }
}
```
</div>

کاملاً مناسب است.

[بازگشت به بالا](#top)

---

<a id="when-bad"></a>
## Default Method چه زمانی بد است؟

این Default Method بد است:

<div dir="ltr">

```java
default void save() {
    database.save(this);  // ❌ Interface نباید Database بشناسد
}
```
</div>

یا:

<div dir="ltr">

```java
default void publish() {
    kafka.send(...);  // ❌ Interface نباید Kafka بشناسد
}
```
</div>

یا:

<div dir="ltr">

```java
default void lock() {
    mutex.lock();  // ❌ Interface نباید Lock را فرض کند
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="golden-rule"></a>
## قانون مهم

Default Method نباید:

- State خاصی فرض کند
- Thread Model خاصی فرض کند
- Synchronization خاصی فرض کند
- Performance خاصی فرض کند
- Storage خاصی فرض کند

[بازگشت به بالا](#top)

---

<a id="for-new-implementations"></a>
## Default Method برای کمک به پیاده‌سازی‌های جدید

کتاب یک نکته مهم دیگر می‌گوید: Default Method برای این ساخته نشده که Interface را مرتب تغییر بدهیم. بلکه برای **کمک به پیاده‌سازی Interfaceهای جدید**.

مثلاً:

<div dir="ltr">

```java
interface Cache<K,V> {
    default boolean isEmpty() {
        return size() == 0;
    }
}
```
</div>

از همان ابتدا عالی است.

ولی بعد از پنج سال اضافه کردن `default void clearExpired()` ممکن است صدها Implementation را خراب کند.

[بازگشت به بالا](#top)

---

<a id="can-remove"></a>
## آیا می‌توان Method را حذف کرد؟

**خیر.**

فرض کنید:

<div dir="ltr">

```java
interface UserService {
    void login();
    void logout();
}
```
</div>

اگر `logout()` را حذف کنید، صدها پروژه دیگر Compile نمی‌شوند.

[بازگشت به بالا](#top)

---

<a id="can-change"></a>
## آیا می‌توان Signature را تغییر داد؟

مثلاً `login()` را تبدیل کنیم به `login(String otp)`.

**خیر.** تمام Clientها خراب می‌شوند.

[بازگشت به بالا](#top)

---

<a id="immutable-interface"></a>
## Interface تقریباً Immutable است

به همین دلیل کتاب می‌گوید:

> Interface مانند قرارداد حقوقی است.

بعد از انتشار، تقریباً دیگر تغییر نمی‌کند.

[بازگشت به بالا](#top)

---

<a id="how-to-design"></a>
## چگونه Interface طراحی کنیم؟

کتاب پیشنهاد می‌کند:

حداقل **سه Implementation مختلف** بنویسید.

مثلاً:

```
FileStorage
S3Storage
DatabaseStorage
```

اگر هر سه راحت Interface را پیاده‌سازی کردند، احتمالاً Interface خوب طراحی شده است.

سپس چند Client مختلف بنویسید. مثلاً:

```
Spring Boot Service
CLI
Batch Job
```

اگر همه راحت استفاده کردند، Interface بالغ‌تر شده است.

[بازگشت به بالا](#top)

---

<a id="connection"></a>
## ارتباط با Itemهای قبلی

| Item | ارتباط با Item 21 |
|------|-------------------|
| Item 18 | Composition باعث می‌شود تغییر Interface کمتر دردسرساز شود |
| Item 19 | اگر Interface را منتشر کردی، باید آینده‌نگر باشی |
| Item 20 | Interface بهترین Type است، اما باید با دقت طراحی شود |
| Item 21 | بعد از انتشار Interface، تغییر آن بسیار پرهزینه است |

[بازگشت به بالا](#top)

---

<a id="best-practices"></a>
## Best Practices

| قانون | توضیح |
|-------|-------|
| **Interface را کوچک نگه دارید** | اصل Interface Segregation (ISP) |
| **فقط رفتارهای پایدار و عمومی** | در Interface قرار دهید |
| **Default Method برای رفتارهای کاملاً عمومی** | مستقل از پیاده‌سازی |
| **قبل از انتشار، تست کنید** | با چند پیاده‌سازی و چند سناریوی استفاده |
| **به نسخه‌های آینده فکر کنید** | حذف یا تغییر متدهای Interface معمولاً امکان‌پذیر نیست |

[بازگشت به بالا](#top)

---

<a id="anti-patterns"></a>
## Anti-Patternها

| Anti-Pattern | دلیل |
|--------------|------|
| Default Method وابسته به وضعیت داخلی | Interface نباید از جزئیات پیاده‌سازی بداند |
| Default Method وابسته به Lock/Synchronization | Thread Model خاصی را فرض کرده است |
| Default Method وابسته به Database/Network | پیاده‌سازی‌های مختلف را خراب می‌کند |
| تغییر Signature یا حذف متد | همه Clientها خراب می‌شوند |
| Interface بزرگ و همه‌کاره | مجبور به تغییر مداوم می‌شود |

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## جمع‌بندی نهایی

پیام اصلی **Item 21** این است که **Interface یک قرارداد بلندمدت است، نه صرفاً مجموعه‌ای از متدها**. انتشار یک Interface عمومی، تعهدی برای حفظ سازگاری با تمام پیاده‌سازی‌ها و کلاینت‌های آینده ایجاد می‌کند.

### سه اصل کلیدی

| اصل | توضیح |
|-----|-------|
| **۱. Interfaceها تقریباً تغییرناپذیرند** | پس از انتشار، تغییر یا حذف متدها تقریباً غیرممکن است |
| **۲. Default Methodها ریسک‌پذیرند** | تنها برای رفتارهای کاملاً عمومی و مستقل از وضعیت داخلی استفاده شوند |
| **۳. قبل از انتشار، Interface را آزمایش کنید** | حداقل سه پیاده‌سازی و چند سناریوی استفاده مختلف |

### قانون طلایی

> هر Default Method جدید باید با این فرض طراحی شود که ممکن است روی صدها پیاده‌سازی ناشناخته اجرا شود؛ بنابراین تنها رفتارهای کاملاً عمومی، بدون وابستگی به وضعیت داخلی یا جزئیات پیاده‌سازی، برای Default Method مناسب هستند. در معماری‌های مدرن (Spring Boot، Quarkus، Microservices)، یک Interface خوب باید **کوچک، پایدار، آینده‌نگر و قابل توسعه** باشد، زیرا هزینه‌ی تغییر آن پس از انتشار بسیار بیشتر از هزینه‌ی طراحی دقیق اولیه است.

---

[بازگشت به بالا](#top)

</div>
```