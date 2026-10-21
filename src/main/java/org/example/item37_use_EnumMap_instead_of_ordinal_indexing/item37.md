<div dir="rtl">

<a id="top"></a>

# آیتم ۳۷: به‌جای اندیس‌گذاری با `ordinal()` از `EnumMap` استفاده کنید

## (Use EnumMap instead of ordinal indexing)

موضوع این Item یکی از اصول مهم طراحی API در جاوا است: وقتی می‌خواهیم داده‌ای را بر اساس مقادیر یک `enum` دسته‌بندی یا نگهداری کنیم، به‌جای استفاده از `ordinal()` برای دسترسی به آرایه، از `EnumMap` استفاده کنیم.

این توصیه صرفاً درباره زیباتر شدن کد نیست؛ به سه موضوع مهم در مهندسی نرم‌افزار مربوط می‌شود:

- **Type Safety**: جلوگیری از اشتباه در نگاشت مقادیر و اندیس‌ها.
- **Maintainability**: کاهش هزینه تغییر و توسعه مدل دامنه.
- **Performance**: دستیابی به کارایی نزدیک به آرایه، بدون قربانی کردن خوانایی و ایمنی نوعی.

---

## فهرست مطالب

- [۱. مسئله اصلی: چرا استفاده از `ordinal()` خطرناک است؟](#problem)
- [۲. مثال باغ گیاهان: پیاده‌سازی مبتنی بر آرایه](#garden-example)
  - [مشکل اول: نقض Type Safety](#problem1)
  - [مشکل دوم: ناسازگاری آرایه‌ها با Generics](#problem2)
  - [مشکل سوم: وابستگی پنهان به ترتیب تعریف `enum`](#problem3)
  - [مشکل چهارم: خوانایی و گزارش خروجی](#problem4)
- [۳. راه‌حل پیشنهادی کتاب: استفاده از `EnumMap`](#enummap-solution)
  - [چرا سازنده `EnumMap` به `Class` نیاز دارد؟](#why-class)
- [۴. چرا `EnumMap` سریع است؟](#why-fast)
  - [تفاوت سطح API و پیاده‌سازی داخلی](#api-vs-impl)
- [۵. مقایسه معماری گزینه‌ها](#comparison)
- [۶. نسخه Stream: کوتاه‌تر بودن لزوماً به معنی بهتر بودن نیست](#stream-version)
  - [روش ساده با `groupingBy`](#groupingby)
  - [روش Stream با تعیین صریح `EnumMap`](#explicit-enummap)
  - [تفاوت رفتاری که نباید نادیده گرفته شود](#behavioral-difference)
- [۷. مسئله پیچیده‌تر: نگاشت بین دو مقدار `enum`](#two-enum-mapping)
  - [روش اول: آرایه دوبعدی و `ordinal()`](#2d-array)
  - [چرا آرایه دوبعدی خطرناک‌تر است؟](#why-2d-dangerous)
- [۸. راه‌حل دوم: `EnumMap` تو‌در‌تو](#nested-enummap)
  - [الف) نگهداری مبدأ و مقصد در خود `Transition`](#step-a)
  - [ب) گروه‌بندی بر اساس مبدأ](#step-b)
  - [ج) ساخت نگاشت مقصد به انتقال](#step-c)
  - [د) بازیابی انتقال](#step-d)
- [۹. افزودن `PLASMA`: تفاوت واقعی در هزینه نگهداری](#plasma)
- [۱۰. نکته مهم درباره `null`: آیا نبودن انتقال را با `null` نشان دهیم؟](#null-handling)
- [۱۱. ارتباط این Item با طراحی نرم‌افزار در مقیاس سازمانی](#enterprise)
- [۱۲. جمع‌بندی مهندسی Item 37](#final-summary)

[بازگشت به بالا](#top)

---

<a id="problem"></a>
## ۱. مسئله اصلی: چرا استفاده از `ordinal()` خطرناک است؟

فرض کنید این `enum` را داریم:

<div dir="ltr">

```java
enum LifeCycle {
    ANNUAL,
    PERENNIAL,
    BIENNIAL
}
```
</div>

هر مقدار `enum` دارای یک شماره ترتیبی است که از صفر شروع می‌شود:

| مقدار | نتیجه `ordinal()` |
|--------|------------------|
| `ANNUAL` | 0 |
| `PERENNIAL` | 1 |
| `BIENNIAL` | 2 |

<div dir="ltr">

```java
LifeCycle.ANNUAL.ordinal();     // 0
LifeCycle.PERENNIAL.ordinal();  // 1
LifeCycle.BIENNIAL.ordinal();   // 2
```
</div>

نکته مهم این است که `ordinal()` موقعیت یک مقدار در تعریف `enum` را برمی‌گرداند، نه یک شناسه تجاری یا مفهومی پایدار.

برای مثال، اگر تعریف `enum` را به شکل زیر تغییر دهیم:

<div dir="ltr">

```java
enum LifeCycle {
    ANNUAL,
    BIENNIAL,
    PERENNIAL
}
```
</div>

شماره `PERENNIAL` از `1` به `2` تغییر می‌کند؛ حتی اگر معنای این مقدار در دامنه کسب‌وکار هیچ تغییری نکرده باشد.

این ویژگی در کنار آرایه‌ها، زمینه‌ساز خطا می‌شود.

[بازگشت به بالا](#top)

---

<a id="garden-example"></a>
## ۲. مثال باغ گیاهان: پیاده‌سازی مبتنی بر آرایه

کتاب کلاسی به نام `Plant` تعریف می‌کند:

<div dir="ltr">

```java
class Plant {
    enum LifeCycle {
        ANNUAL, PERENNIAL, BIENNIAL
    }

    final String name;
    final LifeCycle lifeCycle;

    Plant(String name, LifeCycle lifeCycle) {
        this.name = name;
        this.lifeCycle = lifeCycle;
    }

    @Override
    public String toString() {
        return name;
    }
}
```
</div>

هدف این است که گیاهان یک باغ را بر اساس چرخه زندگی‌شان گروه‌بندی کنیم:

<div dir="ltr">

```
ANNUAL: [Marigold, Petunia]
PERENNIAL: [Rose, Lavender]
BIENNIAL: [Foxglove]
```
</div>

### روش اول: استفاده از آرایه‌ای از مجموعه‌ها

<div dir="ltr">

```java
Set<Plant>[] plantsByLifeCycle =
    (Set<Plant>[]) new Set[Plant.LifeCycle.values().length];

for (int i = 0; i < plantsByLifeCycle.length; i++) {
    plantsByLifeCycle[i] = new HashSet<>();
}

for (Plant p : garden) {
    plantsByLifeCycle[p.lifeCycle.ordinal()].add(p);
}
```
</div>

ارتباط میان شماره آرایه و چرخه زندگی صرفاً بر اساس قرارداد برنامه‌نویس برقرار شده است؛ خود آرایه از این ارتباط اطلاعی ندارد.

<a id="problem1"></a>
### مشکل اول: نقض Type Safety

<div dir="ltr">

```java
plantsByLifeCycle[1].add(plant);
plantsByLifeCycle[Plant.LifeCycle.PERENNIAL.ordinal()].add(plant);
```
</div>

در عبارت اول، کامپایلر نمی‌داند عدد `1` به چه چرخه‌ای اشاره می‌کند. ممکن است برنامه‌نویس اشتباهاً گیاه را در گروه `BIENNIAL` قرار دهد، بدون اینکه کامپایلر بتواند این اشتباه معنایی را تشخیص دهد.

**اصل معماری:** وقتی مفهوم دامنه از نوع `enum` است، بهتر است این نوع در قرارداد ساختار داده حفظ شود، نه اینکه به یک عدد بدون معنا تبدیل شود.

<a id="problem2"></a>
### مشکل دوم: ناسازگاری آرایه‌ها با Generics

<div dir="ltr">

```java
Set<Plant>[] plantsByLifeCycle =
    (Set<Plant>[]) new Set[Plant.LifeCycle.values().length];
```
</div>

شامل یک cast ناامن است. آرایه‌ها در جاوا نسبت به نوع عناصر خود در زمان اجرا آگاهی دارند؛ اما اطلاعات نوع پارامتری Generics مانند `Set<Plant>` در زمان اجرا به‌طور کامل در دسترس نیست (Type Erasure).

<div dir="ltr">

```java
// Does not compile
Set<Plant>[] sets = new Set<Plant>[10];
```
</div>

در نتیجه، برنامه‌نویس مجبور می‌شود آرایه‌ای خام از `Set` بسازد و آن را cast کند. این cast معمولاً باعث هشدار `unchecked cast` می‌شود.

> **نکته ظریف:** این هشدار به معنای خراب بودن قطعی داده‌ها نیست؛ بلکه کامپایلر نمی‌تواند درستی نوع را به‌طور کامل اثبات کند.

<a id="problem3"></a>
### مشکل سوم: وابستگی پنهان به ترتیب تعریف `enum`

فرض کنید شخص دیگری این تغییر را اعمال کند:

<div dir="ltr">

```java
enum LifeCycle {
    PERENNIAL,
    ANNUAL,
    BIENNIAL
}
```
</div>

اگر تمام دسترسی‌ها از طریق `ordinal()` و جدول‌های متناظر انجام شوند، ممکن است بعضی قسمت‌ها همچنان درست کار کنند؛ اما هر جایی که از اندیس‌ها به‌صورت دستی یا با فرض ترتیب قبلی استفاده شده باشد، در معرض خطاست.

<a id="problem4"></a>
### مشکل چهارم: خوانایی و گزارش خروجی

<div dir="ltr">

```java
for (int i = 0; i < plantsByLifeCycle.length; i++) {
    System.out.printf(
        "%s: %s%n",
        Plant.LifeCycle.values()[i],
        plantsByLifeCycle[i]
    );
}
```
</div>

یعنی رابطه بین اندیس و چرخه زندگی را باید دوباره بازسازی کنیم. در نتیجه، یک مفهوم واحد در دو ساختار جداگانه نگهداری می‌شود: آرایه مجموعه‌ها و ترتیب مقادیر `enum`.

[بازگشت به بالا](#top)

---

<a id="enummap-solution"></a>
## ۳. راه‌حل پیشنهادی کتاب: استفاده از `EnumMap`

`EnumMap` یک پیاده‌سازی تخصصی از `Map` در Java است که کلیدهای آن باید از یک نوع `enum` باشند.

<div dir="ltr">

```java
Map<Plant.LifeCycle, Set<Plant>> plantsByLifeCycle =
    new EnumMap<>(Plant.LifeCycle.class);
```
</div>

### بازنویسی کامل مثال

<div dir="ltr">

```java
Map<Plant.LifeCycle, Set<Plant>> plantsByLifeCycle =
    new EnumMap<>(Plant.LifeCycle.class);

for (Plant.LifeCycle lifeCycle : Plant.LifeCycle.values()) {
    plantsByLifeCycle.put(lifeCycle, new HashSet<>());
}

for (Plant plant : garden) {
    plantsByLifeCycle.get(plant.lifeCycle).add(plant);
}

System.out.println(plantsByLifeCycle);
```
</div>

به‌جای `plantsByLifeCycle[1]` می‌گوییم:

<div dir="ltr">

```java
plantsByLifeCycle.get(Plant.LifeCycle.PERENNIAL)
```
</div>

در این حالت، نوع کلید مستقیماً معنای داده را مشخص می‌کند.

<a id="why-class"></a>
### چرا سازنده `EnumMap` به `Class` نیاز دارد؟

<div dir="ltr">

```java
new EnumMap<>(Plant.LifeCycle.class)
```
</div>

علت این است که `EnumMap` باید بداند نوع دقیق کلیدها چیست. این اطلاعات برای ساختار داخلی آن لازم است، به‌خصوص زمانی که نقشه هنوز هیچ کلیدی ندارد.

از آنجا که Generics در جاوا از Type Erasure استفاده می‌کند، صرفاً دانستن نوع پارامتری در زمان کامپایل برای بازیابی نوع واقعی کلید در زمان اجرا کافی نیست.

عبارت `Plant.LifeCycle.class` یک نمونه از **Bounded Type Token** است (ارتباط با Item 33).

> **نکته مهم:** `EnumMap` تنها کلیدهایی از نوع `enum` مشخص‌شده را می‌پذیرد و کلید `null` را قبول نمی‌کند؛ هرچند می‌تواند مقدار `null` داشته باشد.

[بازگشت به بالا](#top)

---

<a id="why-fast"></a>
## ۴. چرا `EnumMap` سریع است؟

یکی از نکات مهم کتاب این است که `EnumMap` در داخل خود از آرایه استفاده می‌کند.

<a id="api-vs-impl"></a>
### تفاوت سطح API و پیاده‌سازی داخلی

| Array-based | EnumMap |
|-------------|---------|
| اندیس عددی، قرارداد دستی، cast ناامن و ارتباط معنایی ضعیف | کلید enum، API نوع‌امن و مدیریت اندیس‌ها توسط پیاده‌سازی |

هر دو می‌توانند از آرایه بهره ببرند؛ تفاوت اصلی در این است که در `EnumMap`، جزئیات اندیس‌گذاری مسئولیت کتابخانه است، نه برنامه‌نویس.

```
enum key
   │
   ▼
internal array index
   │
   ▼
stored value
```

**مزایای این طراحی:**

- نیازی به محاسبه دستی `ordinal()` نداریم
- نیازی به cast ناامن برای ساخت آرایه جنریک نداریم
- نوع کلید در API حفظ می‌شود
- احتمال خطای ناشی از اندیس‌گذاری کاهش می‌یابد
- هزینه دسترسی معمولاً بسیار نزدیک به دسترسی آرایه‌ای است

> **توجه:** «قابل مقایسه بودن سرعت» به معنی تضمین یکسان بودن زمان اجرا در تمام شرایط نیست. نتیجه واقعی به حجم داده، الگوی دسترسی، عملیات انجام‌شده و جزئیات JVM وابسته است.

[بازگشت به بالا](#top)

---

<a id="comparison"></a>
## ۵. مقایسه معماری گزینه‌ها

| معیار | آرایه با `ordinal()` | `HashMap` | `EnumMap` |
|--------|---------------------|-----------|-----------|
| ایمنی نوعی کلید | ضعیف | مناسب | بسیار مناسب |
| نیاز به cast ناامن | معمولاً بله | خیر | خیر |
| کارایی دسترسی | بسیار بالا | معمولاً پایین‌تر | معمولاً نزدیک به آرایه |
| خوانایی | پایین‌تر | خوب | بسیار خوب |
| وابستگی به ترتیب `enum` | مستقیم | خیر | خیر |
| هزینه نگهداری | بالاتر | متوسط | پایین |
| مناسب برای کلیدهای `enum` | قابل استفاده ولی پرریسک | قابل استفاده | انتخاب تخصصی |

> **نکته معماری:** اگر کلیدهای ما از یک مجموعه محدود و مشخص از مقادیر `enum` هستند، استفاده از `EnumMap` معمولاً تصمیم طبیعی‌تری از `HashMap` یا آرایه است.

[بازگشت به بالا](#top)

---

<a id="stream-version"></a>
## ۶. نسخه Stream: کوتاه‌تر بودن لزوماً به معنی بهتر بودن نیست

<a id="groupingby"></a>
### روش ساده با `groupingBy`

<div dir="ltr">

```java
System.out.println(
    Arrays.stream(garden)
        .collect(groupingBy(p -> p.lifeCycle))
);
```
</div>

این کد از نظر خوانایی مناسب است و هدف را به‌صورت مستقیم بیان می‌کند:

> گیاهان را بر اساس چرخه زندگی گروه‌بندی کن.

اما مسئله این است که `groupingBy` در این فرم، پیاده‌سازی `Map` را خودش انتخاب می‌کند. نباید فرض کنیم که نتیجه حتماً `EnumMap` خواهد بود.

<a id="explicit-enummap"></a>
### روش Stream با تعیین صریح `EnumMap`

<div dir="ltr">

```java
System.out.println(
    Arrays.stream(garden)
        .collect(
            groupingBy(
                p -> p.lifeCycle,
                () -> new EnumMap<>(Plant.LifeCycle.class),
                toSet()
            )
        )
);
```
</div>

| پارامتر | نقش |
|---------|-----|
| `p -> p.lifeCycle` | تابع استخراج کلید گروه‌بندی |
| `() -> new EnumMap<>(Plant.LifeCycle.class)` | کارخانه ساخت نقشه نتیجه |
| `toSet()` | نحوه جمع‌آوری عناصر هر گروه |

<a id="behavioral-difference"></a>
### تفاوت رفتاری که نباید نادیده گرفته شود

در نسخه صریح `EnumMap`، ابتدا برای تمام چرخه‌های زندگی مجموعه ایجاد می‌کنیم:

<div dir="ltr">

```java
for (Plant.LifeCycle lifeCycle : Plant.LifeCycle.values()) {
    plantsByLifeCycle.put(lifeCycle, new HashSet<>());
}
```
</div>

پس حتی اگر هیچ گیاه `BIENNIAL` در باغ نباشد، کلید `BIENNIAL` در نقشه وجود دارد و مقدار آن یک مجموعه خالی است.

اما `groupingBy` فقط گروه‌هایی را ایجاد می‌کند که حداقل یک عنصر متناظر داشته باشند.

| چرخه زندگی | نسخه مقداردهی اولیه `EnumMap` | نسخه Stream |
|------------|------------------------------|-------------|
| `ANNUAL` | وجود دارد | وجود دارد |
| `PERENNIAL` | وجود دارد | وجود دارد |
| `BIENNIAL` | وجود دارد، با مجموعه خالی | وجود ندارد |

بنابراین `plantsByLifeCycle.size()` در نسخه مقداردهی اولیه‌شده با `EnumMap` برابر `3` است؛ اما در نسخه Stream برابر `2` خواهد بود.

> **نکته مهندسی:** هنگام refactoring، تنها به یکسان بودن خروجی عناصر توجه نکن؛ رفتار ساختاری API، مانند وجود کلیدهای خالی، نیز می‌تواند بخشی از قرارداد برنامه باشد.

[بازگشت به بالا](#top)

---

<a id="two-enum-mapping"></a>
## ۷. مسئله پیچیده‌تر: نگاشت بین دو مقدار `enum`

مثال کتاب درباره حالت‌های ماده است:

- جامد (`SOLID`)
- مایع (`LIQUID`)
- گاز (`GAS`)

| حالت مبدأ | حالت مقصد | انتقال |
|-----------|-----------|--------|
| `SOLID` | `LIQUID` | `MELT` |
| `LIQUID` | `SOLID` | `FREEZE` |
| `LIQUID` | `GAS` | `BOIL` |
| `GAS` | `LIQUID` | `CONDENSE` |
| `SOLID` | `GAS` | `SUBLIME` |
| `GAS` | `SOLID` | `DEPOSIT` |

<a id="2d-array"></a>
### روش اول: آرایه دوبعدی و `ordinal()`

<div dir="ltr">

```java
private static final Transition[][] TRANSITIONS = {
    { null, MELT, SUBLIME },
    { FREEZE, null, BOIL },
    { DEPOSIT, CONDENSE, null }
};

public static Transition from(Phase from, Phase to) {
    return TRANSITIONS[from.ordinal()][to.ordinal()];
}
```
</div>

<a id="why-2d-dangerous"></a>
### چرا آرایه دوبعدی خطرناک‌تر است؟

فرض کن یک حالت جدید به نام `PLASMA` اضافه شود. تعداد حالت‌ها از ۳ به ۴ افزایش پیدا می‌کند. در نتیجه، آرایه کامل از ۹ خانه به ۱۶ خانه می‌رسد.

$$ \text{Array size} = N^2 $$

**مشکلات اصلی:**

1. **وابستگی به ترتیب:** هر دو اندیس به ترتیب مقادیر `enum` وابسته‌اند.
2. **خطای انسانی:** جابه‌جایی یک مقدار در جدول می‌تواند انتقال اشتباهی را برگرداند.
3. **نگهداری دشوار:** اضافه شدن یک حالت جدید مستلزم بازبینی جدول است.
4. **فضای غیرضروری:** اگر تعداد انتقال‌های معتبر کم باشد، تعداد زیادی خانه استفاده نمی‌شوند.

از همه مهم‌تر، کامپایلر رابطه میان هر خانه و معنای آن را نمی‌فهمد.

[بازگشت به بالا](#top)

---

<a id="nested-enummap"></a>
## ۸. راه‌حل دوم: `EnumMap` تو‌در‌تو

<div dir="ltr">

```java
Map<Phase, Map<Phase, Transition>>
```
</div>

- نقشه بیرونی: از حالت مبدأ به نقشه مقصدها
- نقشه داخلی: از حالت مقصد به نوع انتقال

<a id="step-a"></a>
### الف) نگهداری مبدأ و مقصد در خود `Transition`

<div dir="ltr">

```java
public enum Phase {
    SOLID, LIQUID, GAS;

    public enum Transition {
        MELT(SOLID, LIQUID),
        FREEZE(LIQUID, SOLID),
        BOIL(LIQUID, GAS),
        CONDENSE(GAS, LIQUID),
        SUBLIME(SOLID, GAS),
        DEPOSIT(GAS, SOLID);

        private final Phase from;
        private final Phase to;

        Transition(Phase from, Phase to) {
            this.from = from;
            this.to = to;
        }

        private static final Map<Phase, Map<Phase, Transition>> m =
            Stream.of(values())
                .collect(groupingBy(
                    t -> t.from,
                    () -> new EnumMap<>(Phase.class),
                    toMap(
                        t -> t.to,
                        t -> t,
                        (x, y) -> y,
                        () -> new EnumMap<>(Phase.class)
                    )
                ));

        public static Transition from(Phase from, Phase to) {
            return m.get(from).get(to);
        }
    }
}
```
</div>

<a id="step-b"></a>
### ب) گروه‌بندی بر اساس مبدأ

<div dir="ltr">

```java
groupingBy(
    t -> t.from,
    () -> new EnumMap<>(Phase.class),
    ...
)
```
</div>

این قسمت تمام انتقال‌ها را بر اساس حالت مبدأ گروه‌بندی می‌کند. برای مثال، انتقال‌های `MELT` و `SUBLIME` هر دو در گروه `SOLID` قرار می‌گیرند.

<a id="step-c"></a>
### ج) ساخت نگاشت مقصد به انتقال

<div dir="ltr">

```java
toMap(
    t -> t.to,
    t -> t,
    (x, y) -> y,
    () -> new EnumMap<>(Phase.class)
)
```
</div>

| بخش | نقش |
|------|-----|
| `t -> t.to` | کلید: حالت مقصد |
| `t -> t` | مقدار: خود شیء `Transition` |
| `(x, y) -> y` | تابع ادغام |
| `() -> new EnumMap<>(Phase.class)` | کارخانه نقشه داخلی |

**نکته:** تابع ادغام صرفاً به دلیل overload مورد استفاده لازم است؛ با فرض اینکه برای هر جفت مبدأ و مقصد دقیقاً یک انتقال تعریف شده باشد، در داده معتبر این مثال فراخوانی نمی‌شود. اما اگر دو انتقال با مبدأ و مقصد یکسان تعریف شوند، تابع ادغام مقدار قبلی را با مقدار جدید جایگزین می‌کند. این رفتار ممکن است خطای مدل دامنه را پنهان کند؛ بنابراین در سیستم واقعی باید تصمیم بگیریم که تکراری بودن انتقال مجاز است یا باید خطا ایجاد کند.

<a id="step-d"></a>
### د) بازیابی انتقال

<div dir="ltr">

```java
public static Transition from(Phase from, Phase to) {
    return m.get(from).get(to);
}
```
</div>

از نظر مفهومی، این عملیات دو lookup متوالی است.

[بازگشت به بالا](#top)

---

<a id="plasma"></a>
## ۹. افزودن `PLASMA`: تفاوت واقعی در هزینه نگهداری

دو انتقال جدید:

- `IONIZE`: از `GAS` به `PLASMA`
- `DEIONIZE`: از `PLASMA` به `GAS`

در مدل `EnumMap` کافی است مقادیر جدید را تعریف کنیم:

<div dir="ltr">

```java
public enum Phase {
    SOLID, LIQUID, GAS, PLASMA;

    public enum Transition {
        MELT(SOLID, LIQUID),
        FREEZE(LIQUID, SOLID),
        BOIL(LIQUID, GAS),
        CONDENSE(GAS, LIQUID),
        SUBLIME(SOLID, GAS),
        DEPOSIT(GAS, SOLID),
        IONIZE(GAS, PLASMA),
        DEIONIZE(PLASMA, GAS);

        // سایر اجزای پیاده‌سازی بدون تغییر
    }
}
```
</div>

| معیار | آرایه دوبعدی | `EnumMap` تو‌در‌تو |
|--------|--------------|---------------------|
| افزودن حالت جدید | نیازمند بازبینی جدول | افزودن مقدار و انتقال‌های مرتبط |
| تشخیص خطای ترتیب | محدود | وابستگی به ترتیب حذف شده |
| فضای ساختار | \(O(N^2)\) | متناسب با نگاشت‌های ایجادشده |
| خوانایی مدل دامنه | ضعیف‌تر | بهتر |
| هزینه پیاده‌سازی اولیه | پایین‌تر | بالاتر، مخصوصاً با Stream |

> **نکته:** پیچیدگی زمانی جست‌وجوی دو مرحله‌ای در `EnumMap` معمولاً ثابت است؛ اما مزیت اصلی این راه‌حل، کاهش خطاهای نگهداری است، نه اینکه لزوماً سریع‌تر از آرایه دوبعدی باشد.

[بازگشت به بالا](#top)

---

<a id="null-handling"></a>
## ۱۰. نکته مهم درباره `null`: آیا نبودن انتقال را با `null` نشان دهیم؟

در جدول اولیه، خانه‌های مربوط به انتقال از یک حالت به خودش `null` هستند:

<div dir="ltr">

```java
{ null, MELT, SUBLIME }
```
</div>

اما `null` چند مشکل دارد:

- ممکن است مصرف‌کننده انتظار یک `Transition` معتبر داشته باشد
- دسترسی به نتیجه یا فراخوانی متد روی آن می‌تواند `NullPointerException` ایجاد کند
- تفاوت میان «انتقال تعریف نشده»، «ورودی نامعتبر» و «نبود تغییر حالت» روشن نیست

### راه‌حل‌ها

**اگر انتقال از یک حالت به همان حالت به معنی «بدون تغییر» باشد:**

<div dir="ltr">

```java
public enum Transition {
    NO_CHANGE,
    MELT,
    FREEZE,
    BOIL,
    CONDENSE,
    SUBLIME,
    DEPOSIT
}
```
</div>

البته باید مراقب بود که افزودن `NO_CHANGE` به‌عنوان یک انتقال معمولی، معنای واقعی دامنه را تغییر ندهد.

**یا استفاده از `Optional<Transition>` در API:**

<div dir="ltr">

```java
public static Optional<Transition> find(Phase from, Phase to) {
    Map<Phase, Transition> transitions = m.get(from);
    if (transitions == null) {
        return Optional.empty();
    }
    return Optional.ofNullable(transitions.get(to));
}
```
</div>

> **نکته:** `Optional` مشکل مدل‌سازی را به‌تنهایی حل نمی‌کند؛ ابتدا باید قرارداد رفتار را مشخص کنیم و سپس ابزار مناسب را انتخاب کنیم.

[بازگشت به بالا](#top)

---

<a id="enterprise"></a>
## ۱۱. ارتباط این Item با طراحی نرم‌افزار در مقیاس سازمانی

فرض کن در یک سامانه سفارش، وضعیت‌های زیر را داریم:

<div dir="ltr">

```java
enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
```
</div>

**راه‌حل نامناسب:**

<div dir="ltr">

```java
ordersByStatus[order.status().ordinal()].add(order);
```
</div>

**راه‌حل مناسب‌تر:**

<div dir="ltr">

```java
Map<OrderStatus, List<Order>> ordersByStatus =
    new EnumMap<>(OrderStatus.class);
```
</div>

### ملاحظات اضافی در سیستم‌های سازمانی

| ملاحظه | توضیح |
|--------|-------|
| **Concurrency** | `EnumMap` thread-safe نیست. برای دسترسی هم‌زمان باید سیاست هم‌زمانی مناسب طراحی شود |
| **Lifecycle** | اگر این نقشه یک ساختار موقت برای پردازش درخواست باشد، `EnumMap` انتخاب ساده‌ای است؛ اما اگر داده باید بین نمونه‌های سرویس مشترک باشد، یک ساختار حافظه‌ای محلی کافی نیست |
| **Persistence** | ترتیب `enum` را به‌عنوان شناسه پایدار در دیتابیس ذخیره نکن. برای داده‌های ماندگار، شناسه یا کد صریح و پایدار تعریف کن |
| **Observability** | کلیدهای معنادار `enum` می‌توانند در لاگ‌ها و خروجی‌های تشخیصی خوانایی بیشتری ایجاد کنند |

> **نکته:** اگر تعداد وضعیت‌ها یا قوانین انتقال به‌صورت پویا از پایگاه داده یا پیکربندی بارگذاری می‌شوند، `EnumMap` الزاماً بهترین انتخاب نیست؛ زیرا کلیدهای آن از قبل به یک نوع `enum` مشخص محدود هستند.

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## ۱۲. جمع‌بندی مهندسی Item 37

مهم‌ترین درس این بخش آن است که نباید برای به‌دست آوردن کارایی، معنای نوعی داده را در لایه برنامه از بین ببریم.

| مسئله | تصمیم پیشنهادی |
|--------|----------------|
| نگاشت یک `enum` به یک مقدار | `EnumMap<EnumType, Value>` |
| نگاشت یک `enum` به چند مقدار | `EnumMap<EnumType, Collection<Value>>` |
| نگاشت بین دو `enum` | `EnumMap<EnumType, EnumMap<EnumType, Value>>` |
| گروه‌بندی Stream با نیاز به `EnumMap` | استفاده از overload مناسب `groupingBy` |
| نگهداری شناسه پایدار در دیتابیس | شناسه یا کد صریح، نه `ordinal()` |

### سه اصل نهایی

| اصل | توضیح |
|-----|-------|
| **۱** | `ordinal()` برای ترتیب و موقعیت داخلی مقادیر `enum` است، نه برای ساخت شناسه‌های معنایی در منطق برنامه |
| **۲** | اگر کلیدهای یک نگاشت از نوع `enum` هستند، `EnumMap` معمولاً انتخاب روشن‌تر، امن‌تر و قابل‌نگهداری‌تری است |
| **۳** | اگر رابطه چندبعدی است، از نگاشت‌های تو‌در‌تو استفاده کن تا معنا و نوع داده در ساختار حفظ شوند |

### نتیجه معماری

> **`EnumMap` نمونه‌ای از یک تصمیم طراحی خوب است که جزئیات بهینه‌سازی را در پیاده‌سازی داخلی نگه می‌دارد و در عین حال API نوع‌امن و معناداری در اختیار برنامه‌نویس قرار می‌دهد. این همان تعادلی است که در طراحی کدهای حرفه‌ای جاوا به دنبال آن هستیم: کارایی مناسب، قراردادهای روشن و هزینه نگهداری کمتر.**

---

[بازگشت به بالا](#top)

</div>