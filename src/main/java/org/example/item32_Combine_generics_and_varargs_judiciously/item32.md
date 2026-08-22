<div dir="rtl">

<a id="top"></a>

# آیتم ۳۲: Genericها و Varargs را با دقت ترکیب کنید

## (Combine generics and varargs judiciously)

---

## فهرست مطالب

- [۱. ایده‌ی مرکزی Item](#core-idea)
- [۲. اول Varargs را دقیق بفهمیم](#understanding-varargs)
- [۳. چرا نویسنده می‌گوید Varargs یک "Leaky Abstraction" است؟](#leaky-abstraction)
- [۴. Non-Reifiable Type چیست؟](#non-reifiable)
- [۵. چرا Generic Array مشکل دارد؟](#generic-array-problem)
- [۶. Warning مهم](#the-warning)
- [۷. Heap Pollution دقیقاً چیست؟](#heap-pollution)
- [۸. مثال خطرناک کتاب](#dangerous-example)
- [۹. مرحله اول: Generic Varargs](#step1)
- [۱۰. مرحله دوم: Array Covariance](#step2)
- [۱۱. مرحله سوم: ما یک `List<Integer>` داریم](#step3)
- [۱۲. مرحله چهارم: چرا Exception دیرتر رخ می‌دهد؟](#step4)
- [۱۳. نکته مهم: Cast در Source Code نیست](#implicit-cast)
- [۱۴. چرا Generic Varargs ذاتاً خطرناک است؟](#why-dangerous)
- [۱۵. پس چرا APIهای JDK از Generic Varargs استفاده می‌کنند؟](#why-jdk-uses)
- [۱۶. `@SafeVarargs` چیست؟](#safevarargs)
- [۱۷. `@SafeVarargs` در واقع چه چیزی نیست؟](#what-safevarargs-is-not)
- [۱۸. شرط اول Safe بودن](#condition1)
- [۱۹. شرط دوم: Array نباید Escape کند](#condition2)
- [۲۰. این نکته خیلی مهم است](#important-note)
- [۲۱. چرا `toArray` خطرناک است؟](#toarray-danger)
- [۲۲. مثال پیچیده‌تر `pickTwo`](#picktwo)
- [۲۳. چرا `Object[]` ساخته می‌شود؟](#why-object-array)
- [۲۴. چرا caller warning نمی‌گیرد؟](#no-warning)
- [۲۵. نتیجه مهم](#important-conclusion)
- [۲۶. Generic Varargs Array را به method دیگر بدهیم؟](#passing-array)
- [۲۷. مثال safe کتاب: `flatten`](#flatten-example)
- [۲۸. نکته ظریف درباره `List<? extends T>`](#wildcard-note)
- [۲۹. Alternative بسیار مهم: حذف Generic Varargs](#alternative)
- [۳۰. اما مشکل variable number of arguments چه می‌شود؟](#variable-args)
- [۳۱. چرا این approach بهتر است؟](#why-better)
- [۳۲. مقایسه دو رویکرد](#comparison)
- [۳۳. چه زمانی Varargs انتخاب خوبی است؟](#when-varargs)
- [۳۴. چه زمانی Generic Varargs خطرناک می‌شود؟](#when-dangerous)
- [۳۵. یک Anti-Pattern واقعی](#anti-pattern)
- [۳۶. Design بهتر](#better-design)
- [۳۷. نکته مهم Production](#production-note)
- [۳۸. یک Decision Flow مناسب برای Code Review](#decision-flow)
- [۳۹. تفاوت `@SuppressWarnings` و `@SafeVarargs`](#suppress-vs-safe)
- [۴۰. محدودیت `@SafeVarargs`](#safevarargs-limitation)
- [۴۱. نکته Architect-level: Minimize Unsafe Boundaries](#architect-note)
- [۴۲. ارتباط با Item 27](#connection-item27)
- [۴۳. ارتباط با Item 28](#connection-item28)
- [۴۴. ارتباط با Item 31](#connection-item31)
- [۴۵. یک نکته بسیار مهم درباره `List.of`](#list-of)
- [۴۶. Production-grade recommendation](#production-recommendation)
- [۴۷. جمع‌بندی نهایی](#final-summary)

[بازگشت به بالا](#top)

---

<a id="core-idea"></a>
## ۱. ایده‌ی مرکزی Item

دو قابلیت را داریم:

<div dir="ltr">

```java
Generics
```
</div>

و:

<div dir="ltr">

```java
Varargs
```
</div>

هر دو از Java 5 آمده‌اند، اما این دو abstraction به‌خوبی با هم سازگار نیستند.

دلیل اصلی:

<div dir="ltr">

```text
Generics → Type Erasure
Varargs  → Array
```
</div>

و مشکل اینجاست که:

<div dir="ltr">

```text
Arrays ≠ Generics
```
</div>

از نظر قوانین type system.

بنابراین ترکیب:

<div dir="ltr">

```java
List<String>...
```
</div>

می‌تواند باعث:

<div dir="ltr">

```text
Heap Pollution → Hidden Cast → ClassCastException
```
</div>

شود.

[بازگشت به بالا](#top)

---

<a id="understanding-varargs"></a>
## ۲. اول Varargs را دقیق بفهمیم

وقتی می‌نویسی:

<div dir="ltr">

```java
static void print(String... values) {
    // ...
}
```
</div>

در ظاهر method می‌تواند تعداد متغیری argument بگیرد:

<div dir="ltr">

```java
print("A");
print("A", "B");
print("A", "B", "C");
```
</div>

اما compiler در پشت صحنه باید این arguments را داخل یک array قرار دهد.

مفهوماً:

<div dir="ltr">

```java
print("A", "B", "C");
```
</div>

شبیه این است:

<div dir="ltr">

```java
print(new String[]{"A", "B", "C"});
```
</div>

بنابراین:

<div dir="ltr">

```text
varargs → array
```
</div>

این نکته پایه‌ی کل Item است.

[بازگشت به بالا](#top)

---

<a id="leaky-abstraction"></a>
## ۳. چرا نویسنده می‌گوید Varargs یک "Leaky Abstraction" است؟

در یک abstraction خوب، implementation detail نباید API را تحت تأثیر قرار دهد.

ما به عنوان caller فقط می‌خواهیم بگوییم:

<div dir="ltr">

```java
process(a, b, c);
```
</div>

و نباید نگران باشیم که implementation برای این کار array ساخته است.

اما array واقعاً وجود دارد و type system هم آن را می‌بیند.

در نتیجه این implementation detail در بعضی شرایط "نشت" می‌کند:

<div dir="ltr">

```text
Caller → varargs → Array → Generic Type → Type Erasure
```
</div>

و این همان **leaky abstraction** است.

[بازگشت به بالا](#top)

---

<a id="non-reifiable"></a>
## ۴. Non-Reifiable Type چیست؟

این مفهوم را باید خیلی خوب بلد باشی.

فرض کن:

<div dir="ltr">

```java
List<String>
```
</div>

در compile-time می‌دانیم `List<String>`، اما بعد از Type Erasure، runtime عملاً چیزی شبیه `List` می‌بیند.

یعنی runtime نمی‌تواند به‌صورت عمومی تشخیص دهد `List<String>` از `List<Integer>` است.

به چنین typeهایی می‌گوییم **Non-Reifiable Types**.

مثلاً:

<div dir="ltr">

```java
List<String>
List<Integer>
Map<String, Integer>
List<? extends Number>
```
</div>

اما:

<div dir="ltr">

```java
String
Integer
String[]
int[]
```
</div>

reifiable هستند.

[بازگشت به بالا](#top)

---

<a id="generic-array-problem"></a>
## ۵. چرا Generic Array مشکل دارد؟

Java اجازه نمی‌دهد:

<div dir="ltr">

```java
List<String>[] lists = new List<String>[10]; // ❌
```
</div>

چون runtime نمی‌تواند component type را به شکل لازم مدیریت کند.

اما این:

<div dir="ltr">

```java
static void process(List<String>... lists)
```
</div>

اجازه داده شده است.

این کمی عجیب به نظر می‌رسد:

<div dir="ltr">

```text
new List<String>[10] → ERROR
List<String>...     → WARNING
```
</div>

چرا؟

چون Generic Varargs در عمل کاربرد زیادی دارد. Java designers تصمیم گرفتند:

> این قابلیت را ممنوع نکنیم؛ به programmer warning بدهیم و مسئولیت safety را به او بسپاریم.

[بازگشت به بالا](#top)

---

<a id="the-warning"></a>
## ۶. Warning مهم

ممکن است compiler بگوید:

<div dir="ltr">

```text
Possible heap pollution from parameterized vararg type List<String>
```
</div>

این warning را نباید با یک warning معمولی اشتباه بگیری.

معنی آن تقریباً این است:

> ممکن است یک object با generic type اشتباه وارد ساختار شود و بعداً compiler-generated cast شکست بخورد.

[بازگشت به بالا](#top)

---

<a id="heap-pollution"></a>
## ۷. Heap Pollution دقیقاً چیست؟

فرض کن:

<div dir="ltr">

```java
List<String> strings = ...;
```
</div>

و reference آن somehow به objectای اشاره کند که در واقع `List<Integer>` است.

در این حالت:

<div dir="ltr">

```text
Compile-time: List<String>
Runtime:      List<Integer>
```
</div>

داریم. این اختلاف **Static Type ≠ Runtime Reality** همان Heap Pollution است.

و می‌تواند type safety را بشکند.

[بازگشت به بالا](#top)

---

<a id="dangerous-example"></a>
## ۸. مثال خطرناک کتاب

<div dir="ltr">

```java
static void dangerous(List<String>... stringLists) {
    List<Integer> intList = List.of(42);
    Object[] objects = stringLists;
    objects[0] = intList;
    String s = stringLists[0].get(0);
}
```
</div>

بیایید آن را خط‌به‌خط بررسی کنیم.

[بازگشت به بالا](#top)

---

<a id="step1"></a>
## ۹. مرحله اول: Generic Varargs

داریم:

<div dir="ltr">

```java
List<String>... stringLists
```
</div>

مفهوم source-level: **array of List<String>**

یعنی `List<String>[]`

اما چنین arrayای از نظر runtime مشکل‌دار است.

[بازگشت به بالا](#top)

---

<a id="step2"></a>
## ۱۰. مرحله دوم: Array Covariance

Arrays در Java **covariant** هستند.

مثلاً:

<div dir="ltr">

```java
String[] strings = new String[10];
Object[] objects = strings;
```
</div>

مجاز است. یعنی `String[] <: Object[]`.

به همین دلیل:

<div dir="ltr">

```java
Object[] objects = stringLists;
```
</div>

از نظر compiler قابل قبول است.

[بازگشت به بالا](#top)

---

<a id="step3"></a>
## ۱۱. مرحله سوم: ما یک `List<Integer>` داریم

<div dir="ltr">

```java
List<Integer> intList = List.of(42);
```
</div>

حالا:

<div dir="ltr">

```java
objects[0] = intList;
```
</div>

در نگاه compiler:

<div dir="ltr">

```text
objects → Object[]
```
</div>

و هر چیزی یک `Object` است.

بنابراین assignment ظاهراً قانونی است.

اما واقعیت semantic این است که:

<div dir="ltr">

```text
array expected: List<String>
array now contains: List<Integer>
```
</div>

و این **Heap Pollution** است.

[بازگشت به بالا](#top)

---

<a id="step4"></a>
## ۱۲. مرحله چهارم: چرا Exception دیرتر رخ می‌دهد؟

حالا:

<div dir="ltr">

```java
String s = stringLists[0].get(0);
```
</div>

Compiler به declaration نگاه می‌کند: `List<String>`. بنابراین تصور می‌کند `stringLists[0].get(0)` یک `String` است.

اما runtime object واقعاً `List<Integer>` است. پس مقدار `42` برمی‌گرداند.

Compiler برای assignment به String یک cast ضمنی ایجاد می‌کند:

<div dir="ltr">

```java
String s = (String) stringLists[0].get(0);
```
</div>

و:

<div dir="ltr">

```text
Integer → String
```
</div>

ممکن نیست.

نتیجه: **ClassCastException**

[بازگشت به بالا](#top)

---

<a id="implicit-cast"></a>
## ۱۳. نکته مهم: Cast در Source Code نیست

Source:

<div dir="ltr">

```java
String s = stringLists[0].get(0);
```
</div>

اما bytecode شامل چیزی شبیه:

<div dir="ltr">

```java
String s = (String) stringLists[0].get(0);
```
</div>

است.

پس:

> Generic type system به compiler اجازه می‌دهد castهایی را به‌صورت implicit ایجاد کند.

اگر Heap Pollution اتفاق بیفتد، این castهای مخفی می‌توانند fail شوند.

[بازگشت به بالا](#top)

---

<a id="why-dangerous"></a>
## ۱۴. چرا Generic Varargs ذاتاً خطرناک است؟

> Generic Varargs inherently has a potential type-safety problem.

چون:

<div dir="ltr">

```text
Varargs → Array
Generic → Type Erasure
Array + Erasure → Runtime type information insufficient
```
</div>

بنابراین compiler نمی‌تواند همیشه safety را اثبات کند.

[بازگشت به بالا](#top)

---

<a id="why-jdk-uses"></a>
## ۱۵. پس چرا APIهای JDK از Generic Varargs استفاده می‌کنند؟

چون Generic Varargs همیشه unsafe نیست.

مثلاً:

<div dir="ltr">

```java
Arrays.asList(T... a)
Collections.addAll(Collection<? super T> c, T... elements)
EnumSet.of(E first, E... rest)
```
</div>

می‌توانند به‌صورت safe پیاده‌سازی شوند.

بنابراین قانون:

> Generic Varargs = always bad

**غلط است.**

قانون درست:

> Generic Varargs = potentially unsafe; safety must be established.

[بازگشت به بالا](#top)

---

<a id="safevarargs"></a>
## ۱۶. `@SafeVarargs` چیست؟

از Java 7 داریم:

<div dir="ltr">

```java
@SafeVarargs
```
</div>

مثلاً:

<div dir="ltr">

```java
@SafeVarargs
static <T> List<T> flatten(List<? extends T>... lists) {
    // ...
}
```
</div>

این annotation یک promise است. یعنی programmer می‌گوید:

> من بررسی کرده‌ام که استفاده از varargs array در این method type-safe است.

و compiler در نتیجه warningهای مربوط به call site را suppress می‌کند.

[بازگشت به بالا](#top)

---

<a id="what-safevarargs-is-not"></a>
## ۱۷. `@SafeVarargs` در واقع چه چیزی نیست؟

این بسیار مهم است:

<div dir="ltr">

```java
@SafeVarargs
```
</div>

به این معنی نیست: "من warning را دوست ندارم، پس آن را خاموش کردم."

بلکه:

<div dir="ltr">

```text
@SafeVarargs → Safety Contract
```
</div>

اگر method unsafe باشد، استفاده از annotation اشتباه است.

[بازگشت به بالا](#top)

---

<a id="condition1"></a>
## ۱۸. شرط اول Safe بودن

> Method نباید چیزی در varargs array ذخیره کند.

مثلاً این خطرناک است:

<div dir="ltr">

```java
static <T> void dangerous(T... args) {
    args[0] = ...;
}
```
</div>

چرا؟ چون array ممکن است runtime component type متفاوتی داشته باشد و assignment می‌تواند ساختار را آلوده کند.

قاعده:

<div dir="ltr">

```text
Generic Varargs Array → READ okay → WRITE danger
```
</div>

[بازگشت به بالا](#top)

---

<a id="condition2"></a>
## ۱۹. شرط دوم: Array نباید Escape کند

حتی اگر array را تغییر ندهی، هنوز ممکن است unsafe باشی.

مثلاً:

<div dir="ltr">

```java
static <T> T[] toArray(T... args) {
    return args;
}
```
</div>

اینجا هیچ modification نداریم. اما:

<div dir="ltr">

```text
args → return → caller
```
</div>

reference به array از method خارج می‌شود.

این **Array Escape** است و می‌تواند باعث Heap Pollution شود.

[بازگشت به بالا](#top)

---

<a id="important-note"></a>
## ۲۰. این نکته خیلی مهم است

پس:

<div dir="ltr">

```text
No mutation
```
</div>

به تنهایی کافی نیست.

باید:

<div dir="ltr">

```text
No mutation AND No escape
```
</div>

داشته باشیم.

[بازگشت به بالا](#top)

---

<a id="toarray-danger"></a>
## ۲۱. چرا `toArray` خطرناک است؟

<div dir="ltr">

```java
static <T> T[] toArray(T... args) {
    return args;
}
```
</div>

به نظر harmless می‌آید. اما نوع واقعی array هنگام invocation ساخته می‌شود و generic type information در runtime محدود است.

بنابراین method عملاً دارد:

<div dir="ltr">

```text
Generic Varargs Array → expose to caller
```
</div>

می‌کند. این اجازه می‌دهد مشکل type safety از این method به caller منتقل شود.

[بازگشت به بالا](#top)

---

<a id="picktwo"></a>
## ۲۲. مثال پیچیده‌تر `pickTwo`

کتاب:

<div dir="ltr">

```java
static <T> T[] pickTwo(T a, T b, T c) {
    switch(ThreadLocalRandom.current().nextInt(3)) {
        case 0: return toArray(a, b);
        case 1: return toArray(a, c);
        case 2: return toArray(b, c);
    }
    throw new AssertionError();
}
```
</div>

و caller:

<div dir="ltr">

```java
String[] attributes = pickTwo("Good", "Fast", "Cheap");
```
</div>

در ظاهر کاملاً منطقی است.

اما runtime ممکن است:

<div dir="ltr">

```text
pickTwo → toArray → Object[] → return → compiler expects String[] → implicit cast → ClassCastException
```
</div>

[بازگشت به بالا](#top)

---

<a id="why-object-array"></a>
## ۲۳. چرا `Object[]` ساخته می‌شود؟

در داخل:

<div dir="ltr">

```java
toArray(a, b)
```
</div>

compiler باید arrayای بسازد که برای هر نوع `T`ای که ممکن است در call site استفاده شود، قابل استفاده باشد.

به‌طور عمومی‌ترین انتخاب: `Object[]`

بنابراین ممکن است `T = String` در caller باشد، اما array واقعی `Object[]` باشد.

و:

<div dir="ltr">

```text
Object[] ≠ String[]
```
</div>

از نظر assignment.

[بازگشت به بالا](#top)

---

<a id="no-warning"></a>
## ۲۴. چرا caller warning نمی‌گیرد؟

Caller:

<div dir="ltr">

```java
String[] attributes = pickTwo("Good", "Fast", "Cheap");
```
</div>

کاملاً type-correct به نظر می‌رسد.

Compiler بر اساس signature می‌گوید:

<div dir="ltr">

```text
pickTwo(...) returns T[]
T = String → therefore → String[]
```
</div>

پس warning ندارد.

اما runtime واقعیت: `Object[]`

و compiler برای assignment یک cast implicit دارد:

<div dir="ltr">

```java
String[] attributes = (String[]) pickTwo(...);
```
</div>

این cast fail می‌شود.

[بازگشت به بالا](#top)

---

<a id="important-conclusion"></a>
## ۲۵. نتیجه مهم

این مثال نشان می‌دهد:

> Heap Pollution ممکن است در یک method ایجاد شود ولی exception چند stack frame یا حتی چند method پایین‌تر اتفاق بیفتد.

این موضوع در production debugging مهم است. ممکن است stack trace در `String[] attributes = ...` بترکد، اما root cause در `toArray()` باشد.

[بازگشت به بالا](#top)

---

<a id="passing-array"></a>
## ۲۶. Generic Varargs Array را به method دیگر بدهیم؟

به‌طور کلی unsafe است که generic varargs array را در اختیار method دیگری قرار دهی.

مگر دو حالت خاص:

**حالت ۱:** به یک varargs method دیگری بدهی که خودش correctly annotated شده با `@SafeVarargs`

**حالت ۲:** به یک non-varargs method بدهی که فقط روی contents محاسبه انجام می‌دهد و reference را expose نمی‌کند.

مثلاً:

<div dir="ltr">

```java
static int count(Object[] values) {
    return values.length;
}
```
</div>

اگر صرفاً read-only computation باشد، مشکلی ندارد.

[بازگشت به بالا](#top)

---

<a id="flatten-example"></a>
## ۲۷. مثال safe کتاب: `flatten`

<div dir="ltr">

```java
@SafeVarargs
static <T> List<T> flatten(List<? extends T>... lists) {
    List<T> result = new ArrayList<>();
    for (List<? extends T> list : lists) {
        result.addAll(list);
    }
    return result;
}
```
</div>

چرا safe است؟

چون `lists` فقط خوانده می‌شود. نمی‌کنیم `lists[0] = ...` و نمی‌کنیم `return lists;`.

در عوض:

<div dir="ltr">

```text
lists → read → copy contents → result
```
</div>

[بازگشت به بالا](#top)

---

<a id="wildcard-note"></a>
## ۲۸. نکته ظریف درباره `List<? extends T>`

این قسمت به Item 31 مربوط است.

<div dir="ltr">

```java
List<? extends T>...
```
</div>

یعنی هر listای که producer نوع T باشد.

مثلاً اگر `T = Number`، می‌توانیم داشته باشیم:

<div dir="ltr">

```java
List<Integer>
List<Double>
List<Long>
```
</div>

پس این method همزمان از دو Item استفاده می‌کند:

<div dir="ltr">

```text
Item 31: ? extends T
Item 32: ... + @SafeVarargs
```
</div>

[بازگشت به بالا](#top)

---

<a id="alternative"></a>
## ۲۹. Alternative بسیار مهم: حذف Generic Varargs

اگر می‌توانی، به‌جای:

<div dir="ltr">

```java
List<? extends T>... lists
```
</div>

از:

<div dir="ltr">

```java
List<List<? extends T>> lists
```
</div>

استفاده کن.

مثلاً:

<div dir="ltr">

```java
static <T> List<T> flatten(List<List<? extends T>> lists) {
    List<T> result = new ArrayList<>();
    for (List<? extends T> list : lists) {
        result.addAll(list);
    }
    return result;
}
```
</div>

حالا دیگر:

<div dir="ltr">

```text
Generic + Array
```
</div>

نداریم.

داریم:

<div dir="ltr">

```text
Generic + List
```
</div>

و compiler می‌تواند safety را به‌صورت کامل‌تری بررسی کند.

[بازگشت به بالا](#top)

---

<a id="variable-args"></a>
## ۳۰. اما مشکل variable number of arguments چه می‌شود؟

می‌توانیم از `List.of(...)` استفاده کنیم.

مثلاً:

<div dir="ltr">

```java
flatten(
    List.of(friends, romans, countrymen)
);
```
</div>

در اینجا:

<div dir="ltr">

```text
Caller → List.of(...) → List<List<? extends T>> → flatten()
```
</div>

Generic Array مستقیماً در API ما وجود ندارد.

[بازگشت به بالا](#top)

---

<a id="why-better"></a>
## ۳۱. چرا این approach بهتر است؟

چون safety را compiler اثبات می‌کند.

در روش `@SafeVarargs` می‌گوییم: "Trust me, this is safe."

اما در روش `List<List<? extends T>>` می‌گوییم: "Compiler can verify the type relationship."

این تفاوت از نظر API Design بسیار مهم است.

[بازگشت به بالا](#top)

---

<a id="comparison"></a>
## ۳۲. مقایسه دو رویکرد

| ویژگی | Generic Varargs | List |
|--------|----------------|------|
| Syntax برای caller | ساده | کمی verbose |
| Type Safety | نیازمند analysis | compiler-verifiable |
| Array involvement | ✅ | ❌ |
| `@SafeVarargs` | معمولاً لازم | ❌ |
| احتمال Heap Pollution | وجود دارد | بسیار کمتر |
| Runtime allocation | array | collection/list |
| API simplicity | بهتر | کمی پیچیده‌تر |
| مناسب برای public API | اگر دقیقاً safe باشد | معمولاً امن‌تر |

[بازگشت به بالا](#top)

---

<a id="when-varargs"></a>
## ۳۳. چه زمانی Varargs انتخاب خوبی است؟

مثلاً:

<div dir="ltr">

```java
log("userId", "requestId", "traceId");
```
</div>

یا:

<div dir="ltr">

```java
Set.of(a, b, c)
```
</div>

برای typeهایی که reifiable هستند یا API implementation کاملاً safe است، varargs بسیار مناسب است.

مزیت اصلی: **Excellent call-site ergonomics**

[بازگشت به بالا](#top)

---

<a id="when-dangerous"></a>
## ۳۴. چه زمانی Generic Varargs خطرناک می‌شود؟

این‌ها red flag هستند:

<div dir="ltr">

```java
T...
List<T>...
Map<K,V>...
```
</div>

به‌خصوص اگر داخل method:

<div dir="ltr">

```java
args[0] = ...
return args;
field = args;
```
</div>

داشته باشی.

[بازگشت به بالا](#top)

---

<a id="anti-pattern"></a>
## ۳۵. یک Anti-Pattern واقعی

فرض کن:

<div dir="ltr">

```java
public static <T> T[] collect(T... values) {
    return values;
}
```
</div>

ممکن است caller بنویسد:

<div dir="ltr">

```java
String[] values = collect("A", "B", "C");
```
</div>

API از نظر ظاهری عالی است.

اما underlying array type ممکن است چیزی باشد که API نمی‌تواند تضمین کند.

بنابراین این API یک abstraction خطرناک ایجاد کرده است.

[بازگشت به بالا](#top)

---

<a id="better-design"></a>
## ۳۶. Design بهتر

اگر واقعاً هدف collection کردن values است:

<div dir="ltr">

```java
public static <T> List<T> collect(T... values) {
    return List.of(values);
}
```
</div>

این هنوز generic varargs دارد، اما array را expose نمی‌کند.

یا اگر API flexibility مهم‌تر از varargs syntax است:

<div dir="ltr">

```java
public static <T> List<T> collect(List<T> values) {
    return List.copyOf(values);
}
```
</div>

[بازگشت به بالا](#top)

---

<a id="production-note"></a>
## ۳۷. نکته مهم Production: `@SafeVarargs` باید کوچک و محلی باشد

در یک library production-grade بهتر است `@SafeVarargs` روی یک method کوچک با behavior کاملاً مشخص باشد، نه اینکه method پیچیده‌ای داشته باشیم که در آن:

- array mutate شود
- array به چند component منتقل شود
- reference در field ذخیره شود
- array return شود
- callback به آن دسترسی پیدا کند

هرچه data flow پیچیده‌تر شود، اثبات safety سخت‌تر می‌شود.

[بازگشت به بالا](#top)

---

<a id="decision-flow"></a>
## ۳۸. یک Decision Flow مناسب برای Code Review

وقتی در PR چنین چیزی دیدی:

<div dir="ltr">

```java
<T> method(T... values)
```
</div>

این مسیر را برو:

<div dir="ltr">

```text
                 Generic Varargs?
                       │
                      YES
                       │
                       ▼
             آیا array تغییر می‌کند؟
                 /           \
               YES           NO
                │             │
                ▼             ▼
             UNSAFE      آیا array escape دارد؟
                           /          \
                         YES          NO
                          │            │
                          ▼            ▼
                       UNSAFE        SAFE
                          │            │
                          ▼            ▼
                     Redesign     @SafeVarargs
```
</div>

اگر `UNSAFE` شد، اولویت با redesign است؛ نه `@SuppressWarnings`.

[بازگشت به بالا](#top)

---

<a id="suppress-vs-safe"></a>
## ۳۹. تفاوت `@SuppressWarnings` و `@SafeVarargs`

### `@SuppressWarnings`

<div dir="ltr">

```java
@SuppressWarnings("unchecked")
```
</div>

می‌گوید: "این warning را در این scope نشان نده." هیچ guaranteeای ایجاد نمی‌کند.

### `@SafeVarargs`

<div dir="ltr">

```java
@SafeVarargs
```
</div>

می‌گوید: "این generic varargs method از نظر استفاده از varargs array safe است."

بنابراین:

<div dir="ltr">

```text
@SuppressWarnings → Warning suppression
@SafeVarargs → Safety assertion + API usability
```
</div>

[بازگشت به بالا](#top)

---

<a id="safevarargs-limitation"></a>
## ۴۰. محدودیت `@SafeVarargs`

چرا نمی‌توان روی هر methodی گذاشت؟

چون method قابل override ممکن است implementation متفاوتی داشته باشد.

اگر:

<div dir="ltr">

```java
class Base {
    void process(T... values) {}
}
```
</div>

و subclass آن را override کند، implementation subclass ممکن است unsafe باشد.

بنابراین Java می‌خواهد `@SafeVarargs` فقط جایی باشد که implementation قابل جایگزینی نیست.

در Java 8: `static` و `final` و از Java 9: `private` هم مجاز شد.

[بازگشت به بالا](#top)

---

<a id="architect-note"></a>
## ۴۱. نکته Architect-level: Minimize Unsafe Boundaries

یک اصل مهم که از این Item می‌توان استخراج کرد:

> **Unsafe operation را در کوچک‌ترین boundary ممکن محصور کن.**

مثلاً در یک library:

<div dir="ltr">

```text
                 Public API
                     │
                     ▼
              @SafeVarargs method
                     │
              small controlled area
                     │
                     ▼
                Safe types
```
</div>

نه اینکه:

<div dir="ltr">

```text
Public API → Generic array → many methods → callbacks → mutable state → eventual exception
```
</div>

هرچه unsafe boundary بزرگ‌تر باشد، reasoning سخت‌تر می‌شود.

[بازگشت به بالا](#top)

---

<a id="connection-item27"></a>
## ۴۲. ارتباط با Item 27

Item 27 می‌گفت: "Eliminate unchecked warnings."

Item 32 یک مورد خاص آن را نشان می‌دهد.

اگر compiler گفت: "Possible heap pollution from parameterized vararg type"

نباید فوراً بنویسی `@SuppressWarnings("unchecked")`.

بلکه:

<div dir="ltr">

```text
Warning → Understand why → Prove safety → @SafeVarargs
```
</div>

یا:

<div dir="ltr">

```text
Warning → Cannot prove safety → Redesign API
```
</div>

[بازگشت به بالا](#top)

---

<a id="connection-item28"></a>
## ۴۳. ارتباط با Item 28

Item 28: "Prefer lists to arrays for generic types."

Item 32 عملاً یک application بسیار مهم از همان اصل است.

چون:

<div dir="ltr">

```text
Varargs → Array
Generic → Non-reifiable
Generic Varargs → Generic Array problem
```
</div>

و راه‌حل طبیعی: `List`

[بازگشت به بالا](#top)

---

<a id="connection-item31"></a>
## ۴۴. ارتباط با Item 31

Item 31: "Use bounded wildcards to increase API flexibility."

مثلاً `List<? extends T>` در `flatten`.

پس در APIهای generic واقعی معمولاً این دو مفهوم با هم دیده می‌شوند:

<div dir="ltr">

```java
@SafeVarargs
static <T> List<T> flatten(List<? extends T>... lists)
```
</div>

که:

<div dir="ltr">

```text
? extends T → Item 31
@SafeVarargs + ... → Item 32
```
</div>

[بازگشت به بالا](#top)

---

<a id="list-of"></a>
## ۴۵. یک نکته بسیار مهم درباره `List.of`

در مثال کتاب:

<div dir="ltr">

```java
flatten(List.of(friends, romans, countrymen));
```
</div>

`List.of(...)` خودش variable number of arguments می‌گیرد.

پس سؤال طبیعی این است: "مگر دوباره Generic Varargs نداریم؟"

بله، در implementation/library boundary چنین چیزی وجود دارد، اما نکته این است که Java library آن را در یک API کنترل‌شده و امن encapsulate کرده است.

یعنی:

<div dir="ltr">

```text
Application Code → List.of(...) → Library-controlled varargs → Safe List abstraction → Your code
```
</div>

این همان چیزی است که در طراحی library باید دنبال کنیم:

> **Unsafe/complex mechanism را در یک boundary کوچک و قابل اعتماد encapsulate کن.**

[بازگشت به بالا](#top)

---

<a id="production-recommendation"></a>
## ۴۶. Production-grade recommendation

اگر در یک Code Review چنین APIای ببینم:

<div dir="ltr">

```java
public static <T> Result<T> process(List<T>... inputs)
```
</div>

اول سؤال من این نیست: "آیا compile می‌شود؟"

بلکه:

**سؤال ۱:** آیا واقعاً varargs لازم است؟ اگر نه: `List<List<T>>`

**سؤال ۲:** اگر لازم است، آیا method array را mutate می‌کند؟ اگر بله: Redesign

**سؤال ۳:** آیا array escape می‌کند؟ اگر بله: Redesign

**سؤال ۴:** اگر safe است: `@SafeVarargs`

**سؤال ۵:** آیا performance واقعاً مهم است؟ اگر بله: Benchmark / JMH، نه حدس.

[بازگشت به بالا](#top)

---

<a id="final-summary"></a>
## ۴۷. جمع‌بندی نهایی

کل Item 32 را می‌توان در این مدل ذهنی خلاصه کرد:

<div dir="ltr">

```text
                   Varargs
                      │
                      ▼
                    Array
                      │
                      │
                 Generic Type
                      │
                      ▼
               Type Erasure
                      │
                      ▼
            Potential Heap Pollution
                      │
              ┌───────┴────────┐
              │                │
           Unsafe             Safe
              │                │
              ▼                ▼
          Redesign        @SafeVarargs
              │
              ▼
             List
```
</div>

و سه قانون اصلی:

### قانون ۱

<div dir="ltr">

```text
Generic Varargs ≠ automatically unsafe
Generic Varargs = potentially unsafe
```
</div>

### قانون ۲

Generic Varargs زمانی safe است که:

<div dir="ltr">

```text
1. Don't write into the varargs array
2. Don't let the array escape
```
</div>

یعنی:

<div dir="ltr">

```text
No mutation + No escape = Safe
```
</div>

### قانون ۳

اگر safety را نمی‌توانی به‌وضوح اثبات کنی:

<div dir="ltr">

```java
List<List<? extends T>>
```
</div>

را به:

<div dir="ltr">

```java
List<? extends T>...
```
</div>

ترجیح بده.

---

### مهم‌ترین نکته برای مسیر Senior/Architect

Item 32 در نهایت درباره `varargs` نیست؛ درباره‌ی **مدیریت مرزهای Type Safety** است.

در یک API عمومی، سه سطح داریم:

<div dir="ltr">

```text
                Public API
                    │
             Type-safe boundary
                    │
          Implementation details
                    │
             Unsafe mechanisms
```
</div>

اگر مجبور شدی از mechanism بالقوه unsafe مثل Generic Varargs استفاده کنی، باید آن را:

1. در یک **boundary کوچک محصور کنی**
2. **safety آن را اثبات کنی**
3. با `@SafeVarargs` **contract را اعلام کنی**
4. اجازه ندهی array escape کند
5. و اگر safety قابل اثبات نیست، **API را به List تغییر دهی**

این دقیقاً تفاوت بین کدی است که صرفاً **compile می‌شود** و APIای که می‌توان آن را با اطمینان در یک **library یا production system بزرگ** قرار داد.

---

[بازگشت به بالا](#top)

</div>
