import os

base = r'c:\Users\User\AndroidStudioProjects\ArrowFlow\app\src\main\res\layout'

with open(os.path.join(base, 'overlay_game_status.xml'), 'w', encoding='utf-8') as f:
    f.write('<?xml version="1.0" encoding="utf-8"?>\n')
    f.write('<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"\n')
    f.write('    xmlns:app="http://schemas.android.com/apk/res-auto"\n')
    f.write('    xmlns:tools="http://schemas.android.com/tools"\n')
    f.write('    android:layout_width="match_parent"\n')
    f.write('    android:layout_height="match_parent">\n\n')
    
    # Game Over
    f.write('    <androidx.constraintlayout.widget.ConstraintLayout\n')
    f.write('        android:id="@+id/overlayGameOver"\n')
    f.write('        android:layout_width="match_parent"\n')
    f.write('        android:layout_height="match_parent"\n')
    f.write('        android:background="@color/overlay_lose"\n')
    f.write('        android:visibility="gone">\n\n')
    
    f.write('        <TextView android:id="@+id/tvGameOver" android:layout_width="wrap_content" android:layout_height="wrap_content"\n')
    f.write('            android:text="@string/game_over" android:textColor="@color/error"\n')
    f.write('            app:layout_constraintBottom_toTopOf="@+id/btnRestartGameOver" app:layout_constraintEnd_toEndOf="parent"\n')
    f.write('            app:layout_constraintStart_toStartOf="parent" app:layout_constraintTop_toTopOf="parent"\n')
    f.write('            app:layout_constraintVertical_chainStyle="packed" style="@style/TextAppearance.ArrowFlow.Title" />\n\n')
    
    f.write('        <com.google.android.material.button.MaterialButton android:id="@+id/btnRestartGameOver"\n')
    f.write('            android:layout_width="200dp" android:layout_height="wrap_content"\n')
    f.write('            android:layout_marginTop="@dimen/spacing_32" android:text="@string/restart"\n')
    f.write('            app:layout_constraintBottom_toBottomOf="parent" app:layout_constraintEnd_toEndOf="parent"\n')
    f.write('            app:layout_constraintStart_toStartOf="parent" app:layout_constraintTop_toBottomOf="@+id/tvGameOver"\n')
    f.write('            style="@style/Widget.ArrowFlow.Button.Primary" />\n')
    f.write('    </androidx.constraintlayout.widget.ConstraintLayout>\n\n')
    
    # Level Complete
    f.write('    <androidx.constraintlayout.widget.ConstraintLayout\n')
    f.write('        android:id="@+id/overlayLevelComplete"\n')
    f.write('        android:layout_width="match_parent"\n')
    f.write('        android:layout_height="match_parent"\n')
    f.write('        android:background="@color/overlay_win"\n')
    f.write('        android:visibility="gone">\n\n')
    
    f.write('        <TextView android:id="@+id/tvLevelComplete" android:layout_width="wrap_content" android:layout_height="wrap_content"\n')
    f.write('            android:text="@string/level_complete" android:textColor="@color/star_filled"\n')
    f.write('            app:layout_constraintBottom_toTopOf="@+id/tvCompleteInfo" app:layout_constraintEnd_toEndOf="parent"\n')
    f.write('            app:layout_constraintStart_toStartOf="parent" app:layout_constraintTop_toTopOf="parent"\n')
    f.write('            app:layout_constraintVertical_chainStyle="packed" style="@style/TextAppearance.ArrowFlow.Title" />\n\n')
    
    f.write('        <TextView android:id="@+id/tvCompleteInfo" android:layout_width="wrap_content" android:layout_height="wrap_content"\n')
    f.write('            android:layout_marginTop="@dimen/spacing_8"\n')
    f.write('            app:layout_constraintBottom_toTopOf="@+id/tvStars" app:layout_constraintEnd_toEndOf="parent"\n')
    f.write('            app:layout_constraintStart_toStartOf="parent" app:layout_constraintTop_toBottomOf="@+id/tvLevelComplete"\n')
    f.write('            style="@style/TextAppearance.ArrowFlow.Body" />\n\n')
    
    f.write('        <TextView android:id="@+id/tvStars" android:layout_width="wrap_content" android:layout_height="wrap_content"\n')
    f.write('            android:layout_marginTop="@dimen/spacing_16" android:textColor="@color/star_filled" android:textSize="48sp"\n')
    f.write('            app:layout_constraintBottom_toTopOf="@+id/btnNextLevel" app:layout_constraintEnd_toEndOf="parent"\n')
    f.write('            app:layout_constraintStart_toStartOf="parent" app:layout_constraintTop_toBottomOf="@+id/tvCompleteInfo"\n')
    f.write('            tools:text="⭐⭐⭐" />\n\n')
    
    f.write('        <com.google.android.material.button.MaterialButton android:id="@+id/btnNextLevel"\n')
    f.write('            android:layout_width="240dp" android:layout_height="wrap_content"\n')
    f.write('            android:layout_marginTop="@dimen/spacing_32" android:text="@string/next_level"\n')
    f.write('            app:layout_constraintBottom_toTopOf="@+id/btnRestartComplete" app:layout_constraintEnd_toEndOf="parent"\n')
    f.write('            app:layout_constraintStart_toStartOf="parent" app:layout_constraintTop_toBottomOf="@+id/tvStars"\n')
    f.write('            style="@style/Widget.ArrowFlow.Button.Primary" />\n\n')
    
    f.write('        <com.google.android.material.button.MaterialButton android:id="@+id/btnRestartComplete"\n')
    f.write('            android:layout_width="240dp" android:layout_height="wrap_content"\n')
    f.write('            android:layout_marginTop="@dimen/spacing_12" android:text="@string/restart"\n')
    f.write('            app:layout_constraintBottom_toBottomOf="parent" app:layout_constraintEnd_toEndOf="parent"\n')
    f.write('            app:layout_constraintStart_toStartOf="parent" app:layout_constraintTop_toBottomOf="@+id/btnNextLevel"\n')
    f.write('            style="@style/Widget.ArrowFlow.Button.Secondary" />\n')
    f.write('    </androidx.constraintlayout.widget.ConstraintLayout>\n\n')
    f.write('</FrameLayout>\n')
print('overlay_game_status.xml done')