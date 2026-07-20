<div dir="rtl">

<a id="top"></a>

# آیتم ۳: Singleton - ایجاد یک نمونه منحصربه‌فرد

## فهرست مطالب

- [۱. Singleton چیست و چرا؟](#what-is-singleton)
  - [تعریف و مفهوم](#definition)
  - [دو کاربرد اصلی](#use-cases)
  - [Singleton با Static Class فرق دارد](#singleton-vs-static)
- [۲. روش اول: فیلد public static final](#method1)
  - [مزایا](#method1-pros)
  - [مشکل امنیتی Reflection](#method1-reflection)
- [۳. روش دوم: متد Static Factory](#method2)
  - [مقایسه با روش اول](#method2-comparison)
  - [مزایا](#method2-pros)
- [۴. مشکل Serialization و راه‌حل آن](#serialization)
  - [مشکل اصلی](#serialization-problem)
  - [راه‌حل: متد readResolve](#readresolve)
- [۵. روش سوم (بهترین): Enum Singleton](#enum-singleton)
  - [مزایا](#enum-advantages)
  - [محدودیت](#enum-limitation)
- [جدول مقایسه نهایی](#comparison-table)
- [نتیجه‌گیری کلیدی](#conclusion)

[بازگشت به بالا](#top)

---

<a id="what-is-singleton"></a>
## ۱. Singleton چیست و چرا؟

<a id="definition"></a>
### تعریف و مفهوم

**تعریف:** کلاسی که دقیقاً یک بار نمونه‌سازی (instantiate) می‌شود.

در کل JVM فقط یک Instance از یک کلاس وجود دارد و همه Clientها از همان Instance استفاده می‌کنند.

یعنی:

```
               JVM
                │
        +----------------+
        | ConfigManager  |
        +----------------+
                ▲
        ┌───────┼───────┐
        │       │       │
  Service A Service B Service C
```

همه به یک شیء مشترک اشاره می‌کنند.

<a id="use-cases"></a>
### دو کاربرد اصلی

- **Stateless Object:** مثل یک تابع یا ابزار (Utility) که State اختصاصی ندارد و فقط یک سرویس است
- **Unique System Component:** مثل Connection Pool، Logger، یا Configuration Manager

> ⚠️ **هشدار مهم:** Singleton تست‌پذیری کد را سخت می‌کند! چون نمی‌توانید در تست‌ها یک Mock جایگزین کنید، مگر اینکه کلاس Singleton یک Interface پیاده‌سازی کند.

<a id="singleton-vs-static"></a>
### Singleton با Static Class فرق دارد

خیلی‌ها این دو را یکی می‌دانند.

**Static Utility:**
<div dir="ltr">

```java
Math.max(...)
```
</div>
هیچ Objectی ندارد.

اما **Singleton:**
<div dir="ltr">

```java
ConfigurationManager.INSTANCE
```
</div>
واقعاً یک Object است.

می‌تواند:
- Interface پیاده‌سازی کند
- State داشته باشد
- Dependency داشته باشد
- Polymorphism داشته باشد

[بازگشت به بالا](#top)

---

<a id="method1"></a>
## ۲. روش اول: فیلد public static final
<div dir="ltr">

```java
public class Elvis {
    public static final Elvis INSTANCE = new Elvis();
    
    private Elvis() { 
        // constructor خصوصی
    }
    
    public void leaveTheBuilding() { ... }
}
```
</div>
### چطور کار می‌کند؟

| بخش | توضیح |
|------|-------|
| `private Elvis()` | هیچ‌کس نمی‌تواند از بیرون `new Elvis()` کند |
| `public static final` | یک نمونه ثابت و عمومی در اختیار همه |
| `INSTANCE` | فقط یک بار در زمان بارگذاری کلاس ساخته می‌شود |

<a id="method1-pros"></a>
### مزایا

- ✅ **واضح بودن API:** هر کس کد را ببیند، می‌فهمد این Singleton است
- ✅ **سادگی:** کد کوتاه و خواناست

<a id="method1-reflection"></a>
### مشکل امنیتی Reflection

🔴 **حمله Reflection:** یک کلاینت مخرب می‌تواند با `AccessibleObject.setAccessible(true)` به constructor خصوصی دسترسی پیدا کند و نمونه دوم بسازد!

#### راه‌حل: در constructor چک کنید
<div dir="ltr">

```java
private Elvis() {
    if (INSTANCE != null) {
        throw new IllegalStateException("Instance already exists!");
    }
}
```
</div>
[بازگشت به بالا](#top)

---

<a id="method2"></a>
## ۳. روش دوم: متد Static Factory
<div dir="ltr">

```java
public class Elvis {
    private static final Elvis INSTANCE = new Elvis();
    
    private Elvis() { ... }
    
    public static Elvis getInstance() { 
        return INSTANCE; 
    }
    
    public void leaveTheBuilding() { ... }
}
```
</div>
<a id="method2-comparison"></a>
### تفاوت با روش اول

| ویژگی | فیلد public | Static Factory |
|--------|-------------|----------------|
| دسترسی به نمونه | `Elvis.INSTANCE` | `Elvis.getInstance()` |
| انعطاف‌پذیری | کم | زیاد |
| استفاده به عنوان Supplier | ❌ | ✅ (`Elvis::getInstance`) |

<a id="method2-pros"></a>
### مزایای Static Factory

- ✅ **انعطاف‌پذیری:** می‌توانید بدون تغییر API، رفتار را عوض کنید (مثلاً برای هر Thread یک نمونه جدا بسازید)
- ✅ **Generic Factory:** می‌توانید یک Factory کلی برای Singleton‌ها بنویسید
- ✅ **Method Reference:** `Elvis::getInstance` می‌تواند به عنوان `Supplier<Elvis>` استفاده شود

> 💡 **نتیجه‌گیری نویسنده:** اگر مزایای Static Factory برایتان مهم نیست، روش اول (فیلد public) بهتر است چون ساده‌تر و واضح‌تر است.

[بازگشت به بالا](#top)

---

<a id="serialization"></a>
## ۴. مشکل Serialization و راه‌حل آن

اگر Singleton را Serializable کنید، مشکل بزرگی پیش می‌آید:
<div dir="ltr">

```java
// ❌ این کافی نیست!
public class Elvis implements Serializable { ... }
```
</div>
<a id="serialization-problem"></a>
### مشکل چیست؟

هر بار که یک نمونه Serialized را Deserialize می‌کنید، JVM یک نمونه جدید می‌سازد! یعنی دیگر Singleton نیست 😱

<a id="readresolve"></a>
### راه‌حل: متد readResolve
<div dir="ltr">

```java
private Object readResolve() {
    // نمونه جدید ساخته شده را دور بریز
    // و نمونه اصلی را برگردان
    return INSTANCE;
}
```
</div>

| مرحله | توضیح |
|--------|--------|
| Deserialization | JVM یک نمونه جدید می‌سازد |
| `readResolve()` | فراخوانی می‌شود |
| برگرداندن `INSTANCE` | نمونه جدید توسط Garbage Collector حذف می‌شود |

همچنین باید همه فیلدهای instance را `transient` اعلام کنید.

[بازگشت به بالا](#top)

---

<a id="enum-singleton"></a>
## ۵. روش سوم (بهترین): Enum Singleton ⭐
<div dir="ltr">

```java
public enum Elvis {
    INSTANCE;
    
    public void leaveTheBuilding() { ... }
}
```
</div>
<a id="enum-advantages"></a>
### چرا Enum بهترین روش است؟

| ویژگی | Enum | دو روش دیگر |
|--------|------|-------------|
| میزان کد | خیلی کم | بیشتر |
| Serialization | ✅ خودکار | نیاز به `readResolve` |
| Reflection Attack | ✅ کاملاً ایمن | نیاز به چک دستی |
| Thread Safety | ✅ خودکار | نیاز به `synchronized` یا `volatile` |
| چند نمونه | ❌ غیرممکن | ممکن است با Reflection |

### توضیح عمیق‌تر

- JVM تضمین می‌کند هر Enum Constant فقط یک بار ساخته شود
- مکانیزم Serialization داخلی Enum از تکرار نمونه جلوگیری می‌کند
- Reflection نمی‌تواند Enum را دستکاری کند

<a id="enum-limitation"></a>
### محدودیت

❌ اگر Singleton شما باید از یک کلاس دیگر ارث‌بری (`extends`) کند (غیر از Enum)، نمی‌توانید از این روش استفاده کنید.

✅ اما می‌توانید Interface پیاده‌سازی کنید.

[بازگشت به بالا](#top)

---

<a id="comparison-table"></a>
## جدول مقایسه نهایی

| معیار | فیلد public | Static Factory | Enum ⭐ |
|--------|-------------|----------------|---------|
| سادگی | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| وضوح API | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| انعطاف‌پذیری | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| امنیت Reflection | ⭐⭐ (با چک دستی) | ⭐⭐ (با چک دستی) | ⭐⭐⭐⭐⭐ |
| Serialization | ⭐⭐ (با `readResolve`) | ⭐⭐ (با `readResolve`) | ⭐⭐⭐⭐⭐ |
| Thread Safety | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

[بازگشت به بالا](#top)

---

<a id="conclusion"></a>
## نتیجه‌گیری کلیدی (با زبان خود Joshua Bloch)

> "A single-element enum type is often the best way to implement a singleton."

یعنی: **اگر می‌توانید از Enum استفاده کنید، حتماً استفاده کنید.**

اگر به دلایل خاص (مثل نیاز به `extends`) نمی‌توانید، بین دو روش دیگر بر اساس نیازتان (سادگی vs انعطاف‌پذیری) انتخاب کنید.

---

[بازگشت به بالا](#top)

</div>
```