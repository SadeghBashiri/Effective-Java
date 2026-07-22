<div dir="rtl">

<a id="top"></a>

# آیتم ۵: تزریق وابستگی‌ها (Dependency Injection)

این یکی از **مهم‌ترین Itemهای کل کتاب Effective Java** است. اگر بخواهم فقط **سه Item** را برای یک Java Backend Developer انتخاب کنم، بدون شک این‌ها هستند:

- Item 1 (Static Factory)
- **Item 5 (Dependency Injection)**
- Item 18 (Composition over Inheritance)

دلیلش این است که تقریباً تمام Frameworkهای مدرن Java (Spring, Quarkus, Micronaut, Jakarta CDI, Guice, Dagger) بر پایه همین Item ساخته شده‌اند.

---

## فهرست مطالب

- [بخش اول: مسئله اصلی](#part1)
  - [مشکل کلاس‌های خودساخته](#self-created-dependencies)
  - [اصل مهم جاشوا بلاک](#principle)
  - [نگاه معماری](#architectural-view)
- [چرا Static Utility اشتباه است؟](#static-utility)
  - [مثال واقعی](#real-world-example)
  - [Anti-Pattern شماره ۱](#antipattern1)
- [چرا Singleton اشتباه است؟](#singleton)
  - [Anti-Pattern شماره ۲](#antipattern2)
  - [Anti-Pattern شماره ۳: Setterهای Mutable](#antipattern3)
  - [مثال واقعی دیگر](#real-world-example2)
- [راه‌حل: Constructor Injection](#solution)
  - [وابستگی به Abstraction](#abstraction)
  - [مزیت بزرگ: تست‌پذیری](#testability)
- [جمع‌بندی بخش اول](#summary-part1)

[بازگشت به بالا](#top)

---

<a id="part1"></a>
## بخش اول: مسئله اصلی

### اصلاً Joshua Bloch می‌خواهد چه مشکلی را حل کند؟

بیشتر افراد فکر می‌کنند:

> Dependency Injection یعنی Spring.

این کاملاً اشتباه است.

Joshua Bloch اصلاً درباره Spring صحبت نمی‌کند.

او درباره یک اصل طراحی صحبت می‌کند:

> **کلاس نباید خودش Dependencyهایش را انتخاب یا ایجاد کند.**

<a id="self-created-dependencies"></a>
### مشکل کلاس‌های خودساخته

فرض کن این کلاس را داریم:
<div dir="ltr">

```java
public class SpellChecker {

    private final EnglishDictionary dictionary =
            new EnglishDictionary();

}
```
</div>
به ظاهر هیچ مشکلی ندارد.

اما سؤال: اگر فردا بخواهیم:

- `FrenchDictionary`
- `PersianDictionary`
- `MedicalDictionary`
- `FakeDictionary` (برای تست)

استفاده کنیم چه؟

نمی‌توانیم.

چرا؟

چون `SpellChecker` خودش تصمیم گرفته است.

<a id="principle"></a>
### اصل مهم

Joshua Bloch این جمله را چندین بار در کتاب تکرار می‌کند:

> **Don't let a class create its own dependencies.**

این اصل از OOP بسیار مهم‌تر از خود DI است.

<a id="architectural-view"></a>
### نگاه معماری

فرض کن این معماری را داریم:

```
             SpellChecker
                    │
                    ▼
          EnglishDictionary
```

ارتباط: **Hard-Coded** است.

یعنی `SpellChecker` فقط با `EnglishDictionary` کار می‌کند.

اما ما می‌خواهیم:

```
              SpellChecker
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
English    Persian    Medical
Dictionary Dictionary Dictionary
```

یعنی Dependency باید قابل تعویض باشد.

[بازگشت به بالا](#top)

---

<a id="static-utility"></a>
## چرا Static Utility اشتباه است؟

کتاب مثال زیر را می‌زند:
<div dir="ltr">

```java
public class SpellChecker {

    private static final Lexicon dictionary = // ...

}
```
</div>
ظاهرش خوب است.

اما سؤال: چند Dictionary می‌توانیم داشته باشیم؟

فقط **یک عدد**.

فرض کن:

- User A → English
- User B → German
- User C → Medical

همه مجبورند از همان Dictionary استفاده کنند.

<a id="real-world-example"></a>
### مثال واقعی

فرض کن `CurrencyFormatter` نوشته‌ای:
<div dir="ltr">

```java
private static final Locale locale = Locale.US;
```
</div>
حالا:

- کاربر ژاپنی؟
- کاربر آلمانی؟
- کاربر ایرانی؟

همه `Locale.US` می‌گیرند.

کاملاً اشتباه است.

<a id="antipattern1"></a>
### Anti-Pattern شماره ۱
<div dir="ltr">

```java
public class CurrencyFormatter {

    private static final Locale locale = Locale.US;

    public static String format(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(locale).format(amount);
    }
}
```
</div>
این کلاس به `Locale.US` گره خورده است و هیچ راهی برای تغییر آن وجود ندارد.

[بازگشت به بالا](#top)

---

<a id="singleton"></a>
## چرا Singleton هم اشتباه است؟

خیلی‌ها فکر می‌کنند:

```
Static Utility → Singleton
```

راه‌حل است.

Joshua Bloch می‌گوید: **نه.**

مثلاً:
<div dir="ltr">

```java
public class SpellChecker {

    private final Lexicon dictionary = new EnglishDictionary();

    public static final SpellChecker INSTANCE =
            new SpellChecker();

}
```
</div>
باز هم Dictionary ثابت است.

تنها تفاوت: `Object` → `Singleton` شده.

اما انعطاف‌پذیری: **صفر** است.

<a id="antipattern2"></a>
### Anti-Pattern شماره ۲

```
OrderService
    ↓
EmailService.INSTANCE
```

بعداً SMTP عوض شد.

نمی‌توانی.

<a id="antipattern3"></a>
### Anti-Pattern شماره ۳: Setterهای Mutable

کتاب مثال جالبی می‌زند.

بعضی افراد می‌گویند: Dictionary را Mutable کنیم.
<div dir="ltr">

```java
spellChecker.setDictionary(...)
```
</div>
ظاهرش خوب است. اما...

فرض کن:

- Thread A → English
- Thread B → French

همزمان اجرا شوند.

هر دو `setDictionary(...)` را صدا بزنند.

نتیجه؟ **Race Condition**

به همین دلیل Joshua Bloch می‌گوید:

```
Awkward
Error-Prone
Concurrent Unsafe
```

<a id="real-world-example2"></a>
### مثال واقعی دیگر
<div dir="ltr">

```java
class CurrencyFormatter {

    private Locale locale;

    void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String format(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(locale).format(amount);
    }
}
```
</div>
- Thread 1: `US`
- Thread 2: `Japan`

فرمت خروجی غیرقابل پیش‌بینی می‌شود.

[بازگشت به بالا](#top)

---

<a id="solution"></a>
## راه‌حل: Constructor Injection

Dependency باید هنگام ساخت Object مشخص شود. نه بعداً.

یعنی:
<div dir="ltr">

```java
new SpellChecker(dictionary)
```
</div>
نه:
<div dir="ltr">

```java
spellChecker.setDictionary(...)
```
</div>
این همان چیزی است که Joshua Bloch پیشنهاد می‌کند:
<div dir="ltr">

```java
public class SpellChecker {

    private final Lexicon dictionary;

    public SpellChecker(Lexicon dictionary) {
        this.dictionary = Objects.requireNonNull(dictionary);
    }

}
```
</div>
<a id="abstraction"></a>
### وابستگی به Abstraction

حالا:

```
SpellChecker
    ↓
Lexicon
```

وابسته است.

نه `EnglishDictionary`.

این همان اصل مهم **Dependency Inversion Principle (DIP)** از SOLID است، هرچند Joshua Bloch نامی از SOLID نمی‌برد. کلاس به **Abstraction (`Lexicon`)** وابسته است، نه به پیاده‌سازی خاص.

نتیجه:

```
SpellChecker
    ↓
Lexicon
    ↓
English
```

یا

```
French
```

یا

```
Persian
```

یا

```
Medical
```

همه قابل استفاده هستند.

<a id="testability"></a>
### مزیت بزرگ: تست‌پذیری

**Production:**
<div dir="ltr">

```java
SpellChecker checker = new SpellChecker(new EnglishDictionary());
```
</div>
**Test:**
<div dir="ltr">

```java
SpellChecker checker = new SpellChecker(new FakeDictionary());
```
</div>
بدون هیچ تغییری در کلاس.

[بازگشت به بالا](#top)

---

<a id="summary-part1"></a>
## جمع‌بندی بخش اول

Joshua Bloch در این قسمت یک اصل بنیادی را مطرح می‌کند:

> **کلاس نباید خودش منابع (Resources) یا وابستگی‌هایش را بسازد یا انتخاب کند.**

اگر کلاس خودش وابستگی را ایجاد کند:

- به یک پیاده‌سازی خاص گره می‌خورد
- انعطاف‌پذیری از بین می‌رود
- تست‌پذیری کاهش می‌یابد

در مقابل، اگر وابستگی از بیرون تزریق شود، همان کلاس می‌تواند با پیاده‌سازی‌های مختلف، بدون تغییر در کد خود، کار کند.

---

| روش | انعطاف‌پذیری | تست‌پذیری | Thread-Safe |
|------|-------------|-----------|-------------|
| Static Utility | ❌ | ❌ | ✅ |
| Singleton | ❌ | ❌ | ✅ |
| Setter Injection | ⚠️ | ✅ | ❌ |
| **Constructor Injection** | ✅ | ✅ | ✅ |

[بازگشت به بالا](#top)

---

</div>
```