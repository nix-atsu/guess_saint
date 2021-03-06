package com.gamesofni.neko.guesswhichsaint.activities;


import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.gamesofni.neko.guesswhichsaint.R;
import com.gamesofni.neko.guesswhichsaint.data.Painting;
import com.gamesofni.neko.guesswhichsaint.data.Saint;
import com.gamesofni.neko.guesswhichsaint.db.PaintingsQuery;
import com.gamesofni.neko.guesswhichsaint.db.SaintsContract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import com.gamesofni.neko.guesswhichsaint.db.SaintsQuery;
import com.github.chrisbanes.photoview.PhotoView;

import static com.gamesofni.neko.guesswhichsaint.db.SaintsContract.CATEGORY_MAGI;
import static com.gamesofni.neko.guesswhichsaint.db.SaintsQuery.CATEGORY_MAGI_KEY;
import static com.gamesofni.neko.guesswhichsaint.db.SaintsQuery.FEMALE_KEY;
import static com.gamesofni.neko.guesswhichsaint.db.SaintsQuery.MALE_KEY;


public class GuessSaint extends AppCompatActivity implements ResetDbDialogFragment.ResetDbDialogListener {

    private static final String USER_CHOICE = "userChoice";
    public static final String CORRECT_SAINT_NAME = "correctSaintName";
    public static final String TAG = GuessSaint.class.getSimpleName();

    private HashSet<Long> saintIds;
    private Map<Long, String> saintIdsToNamesFemale;
    private Map<Long, String> saintIdsToNamesMale;
    private Map<Long, String> saintIdsToNamesMagi;

    private ArrayList<Painting> unguessedPaintings;

    private ArrayList<ToggleButton> buttons;
    private String correctSaintName;

    private int correctChoice;
    private int correctAnswers = 0;
    private int wrongAnswers = 0;
    private boolean hasChecked = false;

    private TextView scoreView;
    PhotoView pictureView;
    private Painting questionPainting;
    private SharedPreferences sharedPreferences;

    private boolean autoNext;
    private static final Random ran = new Random();

    private int correctChoiceColor;
    private int wrongChoiceColor;

    private Toast correctAnswerToast;
    private Toast noAnswerToast;

    private static final String CORRECT_ANSWERS_KEY = "correct";
    private static final String WRONG_ANSWERS_KEY = "wrong";
    public static final String HAS_CHECKED_KEY = "guessed";
    public static final String BUTTON_NAMES = "buttonNames";
    private static final String PAINTING = "painting";
    private static final String CORRECT_CHOICE = "correctChoice";

    public static final int MIN_GUESSES_FOR_SCORE_UPDATE = 5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUp();

        if (saintIds.size() < 4 || unguessedPaintings.size() < 1) {
            return;
        }

        if (savedInstanceState == null) {
            setQuestion();
        }
    }

    private void setUp() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        autoNext = sharedPreferences.getBoolean("autoNext", false);

        Map<String, Map <Long, String>> allSaintsIdToNamesByCategory = SaintsQuery.getAllSaintsIdToNames(this.getApplicationContext());

        saintIdsToNamesFemale = allSaintsIdToNamesByCategory.get(FEMALE_KEY);
        saintIdsToNamesMale = allSaintsIdToNamesByCategory.get(MALE_KEY);
        saintIdsToNamesMagi = allSaintsIdToNamesByCategory.get(CATEGORY_MAGI_KEY);

        saintIds = new HashSet<>();

        saintIds.addAll(saintIdsToNamesFemale.keySet());
        saintIds.addAll(saintIdsToNamesMale.keySet());
        saintIds.addAll(saintIdsToNamesMagi.keySet());

        if (saintIds.size() < 4) {
            setContentView(R.layout.empty_db);
            return;
        }

        unguessedPaintings = PaintingsQuery.getAllUnguessedPaintings(this.getApplicationContext());

        if (unguessedPaintings.size() < 1) {
            setContentView(R.layout.guessed_all_paintings);

            Button resetPaintingsScore = findViewById(R.id.reset_paintings_score);
            resetPaintingsScore.setOnClickListener(
                    view -> {
                        DialogFragment resetConfirmationDialog = new ResetDbDialogFragment();
                        resetConfirmationDialog.show(GuessSaint.this.getFragmentManager(), TAG);
                    }
            );

            return;
        }

        setContentView(R.layout.activity_guess);

        pictureView = findViewById(R.id.guessMergeImageView);
        scoreView = findViewById(R.id.guess_menu_score);

        correctChoiceColor = getResources().getColor(R.color.awesome_green);
        wrongChoiceColor = getResources().getColor(R.color.bad_red);

        setUpButtons();

        Button guessActivityCheckButton = findViewById(R.id.guess_menu_next);
        guessActivityCheckButton.setOnClickListener(this::onSubmitChoice);

        final Button guessActivityBackButton = findViewById(R.id.guess_menu_back);
        guessActivityBackButton.setOnClickListener(view -> GuessSaint.this.onBackPressed());

    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(CORRECT_ANSWERS_KEY)
                && savedInstanceState.containsKey(WRONG_ANSWERS_KEY)) {
            restoreState(savedInstanceState);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void restoreState(Bundle state) {
        correctAnswers = state.getInt(CORRECT_ANSWERS_KEY, 0);
        wrongAnswers = state.getInt(WRONG_ANSWERS_KEY, 0);
        setScore();

        questionPainting = (Painting) state.getSerializable(PAINTING);
        pictureView.setImageResource(questionPainting.getResourceName());

        hasChecked = state.getBoolean(HAS_CHECKED_KEY, false);

        HashMap<Integer, String> buttonNames = (HashMap<Integer, String>) state.getSerializable(BUTTON_NAMES);
        for (Map.Entry<Integer, String> e : buttonNames.entrySet()) {
            setNameOnButton(buttons.get(e.getKey()), e.getValue());
        }
        clearAllButtons();

        correctChoice = state.getInt(CORRECT_CHOICE);
        correctSaintName = state.getString(CORRECT_SAINT_NAME);
        final int userChoice = state.getInt(USER_CHOICE, -1);
        if (userChoice != -1) {
            buttons.get(userChoice).setChecked(true);
            if (hasChecked) {
                if (userChoice != correctChoice) {
                    buttons.get(userChoice).setBackgroundColor(wrongChoiceColor);
                }
                buttons.get(correctChoice).setBackgroundColor(correctChoiceColor);
            }
        }

    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(WRONG_ANSWERS_KEY, wrongAnswers);
        outState.putInt(CORRECT_ANSWERS_KEY, correctAnswers);
        outState.putBoolean(HAS_CHECKED_KEY, hasChecked);
        outState.putInt(CORRECT_CHOICE, correctChoice);
        outState.putString(CORRECT_SAINT_NAME, correctSaintName);
        outState.putInt(USER_CHOICE, getCheckedButtonId());
        outState.putSerializable(PAINTING, questionPainting);

        HashMap<Integer, String> buttonNames = new HashMap<>(4);
        for (int i = 0; i < buttons.size(); i++) {
            String name = buttons.get(i).getText().toString();
            buttonNames.put(i, name);
        }
        outState.putSerializable(BUTTON_NAMES, buttonNames);

        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (wrongAnswers + correctAnswers < MIN_GUESSES_FOR_SCORE_UPDATE) {
            return;
        }

        // save stats to preferences
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();

        // save score to prefs only if got higher
        float savedScore = sharedPreferences.getFloat("score", 0.0f);
        float currentScore = getScore();
        if (savedScore < currentScore) {
            prefEditor.putFloat("score", getScore());
        }

        prefEditor.apply();
    }

    private void setQuestion() {
        questionPainting = unguessedPaintings.get(ran.nextInt(unguessedPaintings.size()));

        final Saint correctSaint = SaintsQuery.getSaint(this, questionPainting.getSaintId());
        this.correctSaintName = correctSaint.getName();

        pictureView.setImageResource(questionPainting.getResourceName());

        correctChoice = ran.nextInt(buttons.size());

        ArrayList<Long> saintsListIds;

        if (correctSaint.getGender().equals(SaintsContract.GENDER_FEMALE)) {
            saintsListIds = new ArrayList<>(saintIdsToNamesFemale.keySet());
        } else if (CATEGORY_MAGI.equals(correctSaint.getCategory())) {
            saintsListIds = new ArrayList<>(saintIdsToNamesMagi.keySet());
        } else {
            saintsListIds = new ArrayList<>(saintIdsToNamesMale.keySet());
        }
        saintsListIds.remove(correctSaint.getId());

        for (int i = 0; i < buttons.size(); i++) {
            ToggleButton button = buttons.get(i);
            if (i == correctChoice) {
                setNameOnButton(button, correctSaint.getName());
                continue;
            }

            final long aSaintId = saintsListIds.remove(ran.nextInt(saintsListIds.size()));
            final String name;

            if (correctSaint.getGender().equals(SaintsContract.GENDER_FEMALE)) {
                name = saintIdsToNamesFemale.get(aSaintId);
            } else if (CATEGORY_MAGI.equals(correctSaint.getCategory())) {
                name = saintIdsToNamesMagi.get(aSaintId);
            } else {
                name = saintIdsToNamesMale.get(aSaintId);
            }

            setNameOnButton(button, name);
        }

        clearAllButtons();
    }

    public void onSubmitChoice(View view) {
        if (hasChecked) {
            onNext();
            return;
        }

        final int userChoiceId = getCheckedButtonId();

        if (userChoiceId == -1) {
            if (noAnswerToast != null) {
                noAnswerToast.cancel();
            }

            noAnswerToast = Toast.makeText(this, R.string.no_answer_toast_text, Toast.LENGTH_SHORT);
            noAnswerToast.show();

            return;
        }
        final boolean isCorrectAnswer = userChoiceId == correctChoice;

        PaintingsQuery.updateCorrectAnswersCount(this, questionPainting.getId(), isCorrectAnswer);

        if (isCorrectAnswer) {
            correctAnswers++;
            if (PaintingsQuery.isCountOverTreshold(this.getApplicationContext(), questionPainting.getId())) {
                unguessedPaintings.remove(new Painting (questionPainting.getId()));
            }
        } else {
            buttons.get(userChoiceId).setBackgroundColor(wrongChoiceColor);
            wrongAnswers++;
        }

        buttons.get(correctChoice).setBackgroundColor(correctChoiceColor);

        setScore();

        if (autoNext) {
            if (correctAnswerToast != null) {
                correctAnswerToast.cancel();
            }
            final String message = isCorrectAnswer ?
                    getString(R.string.answer_correct) :
                    getString(R.string.answer_wrong) + correctSaintName;
            correctAnswerToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
            correctAnswerToast.show();

            onNext();
        } else {
            hasChecked = true;
        }

    }

    public void onNext() {
        hasChecked = false;
        setQuestion();
    }


    private void setUpButtons() {
        CompoundButton.OnCheckedChangeListener onCheckedChangeListener = (button, isChecked) -> {
            if (isChecked) {
                clearAllButtons();
                button.setChecked(true);
            }
        };

        buttons = new ArrayList<>(4);

        setUpButton(onCheckedChangeListener, R.id.guess_button1);
        setUpButton(onCheckedChangeListener, R.id.guess_button2);
        setUpButton(onCheckedChangeListener, R.id.guess_button3);
        setUpButton(onCheckedChangeListener, R.id.guess_button4);

    }

    private void setUpButton(CompoundButton.OnCheckedChangeListener onCheckedChangeListener, int guess_button_id) {
        ToggleButton guessButton = findViewById(guess_button_id);
        guessButton.setOnCheckedChangeListener(onCheckedChangeListener);
        buttons.add(guessButton);
    }

    private void setNameOnButton(ToggleButton button, String name) {
        button.setTextOff(name);
        button.setTextOn(name);
        button.setText(name);
    }

    private void clearAllButtons() {
        for (ToggleButton button : buttons) {
            button.setBackgroundResource(R.drawable.guess_button);
            button.setChecked(false);
        }
    }

    private int getCheckedButtonId() {
        for (int i = 0; i < buttons.size(); i++) {
            ToggleButton button = buttons.get(i);
            if (button.isChecked()) {
                return i;
            }
        }
        return -1;
    }

    private void setScore() {
        final float score = getScore();
        scoreView.setText(String.format(getString(R.string.score_message), score));
    }

    private float getScore() {
        if (correctAnswers + wrongAnswers == 0) {
            return 0f;
        }
        return 100 * ((float) correctAnswers / (float) (correctAnswers + wrongAnswers));
    }

    @Override
    public void onDialogPositiveClick(ResetDbDialogFragment dialog) {
        PaintingsQuery.reset_counters(getApplicationContext());
        finish();
        startActivity(getIntent());
        Toast.makeText(this, R.string.reset_db_done, Toast.LENGTH_SHORT).show();
    }

}
