# FillInBlankView

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/sonpxp/FillInBlankView)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com/)
[![License](https://img.shields.io/badge/license-MIT-orange.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)

A modern and customizable Android view for creating interactive fill-in-the-blank exercises. Perfect for educational apps, quizzes, and language learning applications.

## 📱 Screenshots

|                                 Main Interface                                 | Input State | Validation |
|:------------------------------------------------------------------------------:|:-----------:|:----------:|
| <img src="images/image1.jpg?raw=true" width="200" style="border-radius: 8px;"> | <img src="images/image1.jpg?raw=true" width="200" style="border-radius: 8px;"> | <img src="images/image1.jpg?raw=true" width="200" style="border-radius: 8px;"> |

## ✨ Key Features

- ✅ **Easy Integration** - Simple XML configuration and programmatic setup
- 🎨 **Highly Customizable** - Colors, sizes, animations, and styling options
- 📱 **Responsive Design** - Automatic text wrapping with FlexboxLayout
- ⌨️ **Smart Input Handling** - Auto-focus, keyboard management, and navigation
- 🔍 **Visual Validation** - Real-time feedback with animations and color coding
- 🌙 **Flexible Hints** - Customizable placeholder texts for better UX
- 🎯 **Multiple Answer Support** - Handle multiple blanks in a single text
- 🚀 **Performance Optimized** - Efficient rendering and memory management

## 🚀 Installation

### Gradle (Kotlin DSL)

Add JitPack repository to your root `build.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to your app `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.sonpxp:FillInBlankView:1.0.0")
}
```

### Gradle (Groovy)

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.sonpxp:FillInBlankView:1.0.0'
}
```

## 📖 Usage

### Basic XML Usage

```xml
<com.sonpxp.fillinblankview.FillInBlankView
    android:id="@+id/fillInBlankView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:fib_text="Hello {{blank}}, welcome to {{blank}} development!"
    app:fib_answers='["World", "Android"]'
    app:fib_textSize="16sp"
    app:fib_textColor="@color/black"
    app:fib_editTextColor="@color/blue"
    app:fib_showHint="true"
    app:fib_hintText="..." />
```

### Programmatic Usage

```kotlin
val fillInBlankView = FillInBlankView(this)
fillInBlankView.setContent(
    text = "The capital of {{blank}} is {{blank}}.",
    answers = listOf("France", "Paris")
)

// Set listeners
fillInBlankView.setOnAnswersChangedListener { answers ->
    Log.d("FillInBlank", "Current answers: $answers")
}

fillInBlankView.setOnValidationListener { results ->
    val allCorrect = results.all { it }
    Log.d("FillInBlank", "All correct: $allCorrect")
}

// Validate answers
val isAllCorrect = fillInBlankView.validateAnswers()
```

### Advanced Configuration

```xml
<com.sonpxp.fillinblankview.FillInBlankView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:fib_text="Complete the sentence:\nKotlin is {{blank}} and {{blank}}."
    app:fib_answers='["modern", "concise"]'
    app:fib_textSize="18sp"
    app:fib_textColor="#2E2E2E"
    app:fib_editTextColor="#1976D2"
    app:fib_underlineColor="#1976D2"
    app:fib_underlineWidth="2dp"
    app:fib_underlineFocusedWidth="3dp"
    app:fib_correctColor="#4CAF50"
    app:fib_incorrectColor="#F44336"
    app:fib_minEditTextWidth="80dp"
    app:fib_enableShakeAnimation="true"
    app:fib_autoFocusFirst="true"
    app:fib_showHint="true"
    app:fib_hintTextArray='["language", "feature"]'
    app:fib_hideUnderlineWhenFilled="true" />
```

## 📋 Attributes Reference

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `fib_text` | String | `""` | Text content with `{{blank}}` placeholders |
| `fib_answers` | String Array (JSON) | `[]` | Correct answers for validation |
| `fib_textSize` | Dimension | `16sp` | Text size for both labels and inputs |
| `fib_textColor` | Color | `#000000` | Color for static text |
| `fib_editTextColor` | Color | `#0000FF` | Color for input text |
| `fib_underlineColor` | Color | `#0000FF` | Color for input underlines |
| `fib_underlineWidth` | Dimension | `1dp` | Width of normal underline |
| `fib_underlineFocusedWidth` | Dimension | `2dp` | Width of focused underline |
| `fib_dashLength` | Dimension | `6dp` | Length of dash segments |
| `fib_dashGap` | Dimension | `3dp` | Gap between dash segments |
| `fib_minEditTextWidth` | Dimension | `60dp` | Minimum width for input fields |
| `fib_editTextPadding` | Dimension | `4dp` | Padding inside input fields |
| `fib_extraWidthPadding` | Dimension | `16dp` | Extra width padding for inputs |
| `fib_correctColor` | Color | `#00FF00` | Color for correct validation |
| `fib_incorrectColor` | Color | `#FF0000` | Color for incorrect validation |
| `fib_enableShakeAnimation` | Boolean | `true` | Enable shake animation for errors |
| `fib_autoFocusFirst` | Boolean | `true` | Auto-focus first input on creation |
| `fib_enableInput` | Boolean | `true` | Enable/disable user input |
| `fib_itemSpacing` | Dimension | `2dp` | Spacing between view elements |
| `fib_showHint` | Boolean | `true` | Show placeholder hints |
| `fib_hintText` | String | `"..."` | Default hint text for all inputs |
| `fib_hintTextArray` | String Array (JSON) | `[]` | Individual hints for each input |
| `fib_hideUnderlineWhenFilled` | Boolean | `false` | Hide underline when input has text |

## 🎯 Public Methods

### Content Management

```kotlin
// Set text with blanks
fillInBlankView.setText("Hello {{blank}}!")

// Set correct answers
fillInBlankView.setAnswers(listOf("World"))

// Set both text and answers
fillInBlankView.setContent("Hello {{blank}}!", listOf("World"))

// Get current user answers
val answers: List<String> = fillInBlankView.getAnswers()

// Clear all answers
fillInBlankView.clearAnswers()
```

### Validation

```kotlin
// Check answers (returns List<Boolean>)
val results: List<Boolean> = fillInBlankView.checkAnswers()

// Validate with visual feedback (returns Boolean)
val allCorrect: Boolean = fillInBlankView.validateAnswers()

// Reset validation state
fillInBlankView.resetValidation()
```

### Focus Management

```kotlin
// Focus specific input by index
fillInBlankView.focusEditText(0)

// Focus first input with keyboard
fillInBlankView.focusFirstEditText()

// Get EditText at specific index
val editText: EditText? = fillInBlankView.getEditTextAt(0)
```

### Configuration

```kotlin
// Enable/disable input
fillInBlankView.setEnableInput(false)
val isEnabled: Boolean = fillInBlankView.isInputEnabled()

// Configure hints
fillInBlankView.setShowHint(true)
fillInBlankView.setHintText("Type here...")
fillInBlankView.setHintTextArray(listOf("First", "Second"))

// Configure underline behavior
fillInBlankView.setHideUnderlineWhenFilled(true)
```

### Event Listeners

```kotlin
// Listen to answer changes
fillInBlankView.setOnAnswersChangedListener { answers ->
    // Handle answer changes
    Log.d("Answers", answers.toString())
}

// Listen to validation results
fillInBlankView.setOnValidationListener { results ->
    // Handle validation feedback
    val correctCount = results.count { it }
    Log.d("Validation", "Correct: $correctCount/${results.size}")
}
```

## 🎨 Styling Examples

### Dark Theme

```xml
<com.sonpxp.fillinblankview.FillInBlankView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:fib_text="Complete this {{blank}} example"
    app:fib_answers='["dark"]'
    app:fib_textColor="#FFFFFF"
    app:fib_editTextColor="#BB86FC"
    app:fib_underlineColor="#BB86FC"
    app:fib_correctColor="#03DAC6"
    app:fib_incorrectColor="#CF6679" />
```

### Minimal Style

```xml
<com.sonpxp.fillinblankview.FillInBlankView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:fib_text="Simple {{blank}} design"
    app:fib_answers='["minimal"]'
    app:fib_underlineWidth="1dp"
    app:fib_hideUnderlineWhenFilled="true"
    app:fib_enableShakeAnimation="false"
    app:fib_showHint="false" />
```

## 🔧 Advanced Features

### Multiple Lines Support

```kotlin
val multiLineText = """
    Line 1: Hello {{blank}}!
    Line 2: Welcome to {{blank}} development.
    Line 3: Enjoy {{blank}} programming!
""".trimIndent()

fillInBlankView.setContent(
    text = multiLineText,
    answers = listOf("World", "Android", "Kotlin")
)
```

### Custom Validation Logic

```kotlin
fillInBlankView.setOnValidationListener { results ->
    results.forEachIndexed { index, isCorrect ->
        if (isCorrect) {
            // Handle correct answer
            showSuccessMessage("Answer ${index + 1} is correct!")
        } else {
            // Handle incorrect answer
            val correctAnswer = fillInBlankView.getAnswers()[index]
            showHint("Try again! Hint: ${correctAnswer.take(2)}...")
        }
    }
}
```

### Dynamic Content Updates

```kotlin
// Update content based on difficulty level
fun updateDifficulty(level: Int) {
    when (level) {
        1 -> fillInBlankView.setContent(
            "Easy: 2 + 2 = {{blank}}",
            listOf("4")
        )
        2 -> fillInBlankView.setContent(
            "Medium: The square root of {{blank}} is 5",
            listOf("25")
        )
        3 -> fillInBlankView.setContent(
            "Hard: {{blank}} × {{blank}} = 144",
            listOf("12", "12")
        )
    }
}
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/dev`)
3. Commit your Changes (`git commit -m 'Add some dev'`)
4. Push to the Branch (`git push origin feature/dev`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**sonpxp** - *Initial work* - [@sonpxp](https://github.com/sonpxp)

## 🙏 Acknowledgments

- Android Jetpack Components for modern Android development
- FlexboxLayout for responsive text wrapping
- Material Design guidelines for UI/UX principles

## 📞 Support

If you have any questions or need help, please:

- Open an [issue](https://github.com/sonpxp/FillInBlankView/issues)
- Check the [documentation](https://github.com/sonpxp/FillInBlankView/wiki)
- Contact: [sonpxp@cloudxanh.com](mailto:sonpxp@cloudxanh.com)

---

<div align="center">
  Made with ❤️ by <a href="https://github.com/sonpxp">sonpxp</a>
</div>