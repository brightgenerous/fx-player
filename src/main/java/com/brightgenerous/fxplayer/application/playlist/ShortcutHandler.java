package com.brightgenerous.fxplayer.application.playlist;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;

class ShortcutHandler implements EventHandler<KeyEvent> {

    public static interface IAdapter {

        void controlDirectoryChooser();

        void controlFileChooser();

        void focusPathText();

        void controlLog();

        void controlLogSnap();

        void controlLogFront();

        void controlLogBack();

        void controlLogAuto();

        void controlTab();

        void controlTabSide();

        void controlVideoInfoSide();

        void controlSpectrums();

        void controlPlayPause();

        void controlPlay();

        void controlPause();

        void controlBack();

        void controlNext();

        void controlTime(long seconds);

        void controlTimePlus(long seconds);

        void controlTimeMinus(long seconds);

        void controlVolume(int volume);

        void controlVolumePlus(int volume);

        void controlVolumeMinus(int volume);

        void controlJump(int value);

        void controlJumpPlus(int value);

        void controlJumpMinus(int value);

        void controlSaveFile();

        void controlSaveImage();

        void controlWindowScreen();

        void controlWindowScreenMin();

        void controlWindowScreenMax();

        void controlWindowFront();

        void controlWindowBack();
    }

    private static final Pattern directoryPattern = Pattern
            .compile("^(?:.*\\s+)?(?:d|dir|directory)\\s*$");

    private static final Pattern filePattern = Pattern.compile("^(?:.*\\s+)?(?:f|fl|file)\\s*$");

    private static final Pattern pathPattern = Pattern.compile("^(?:.*\\s+)?(?:u|url)\\s*$");

    private static final Pattern logPattern = Pattern.compile("^(?:.*\\s+)?(?:l|lg|log)\\s*$");

    private static final Pattern logSnapPattern = Pattern
            .compile("^(?:.*\\s+)?(?:ll|lglg|loglog)\\s*$");

    private static final Pattern logFrontPattern = Pattern
            .compile("^(?:.*\\s+)?(?:lf|lgfr|lgfrnt)\\s*$");

    private static final Pattern logBackPattern = Pattern
            .compile("^(?:.*\\s+)?(?:lb|lgbk|lgbck)\\s*$");

    private static final Pattern logAutoPattern = Pattern
            .compile("^(?:.*\\s+)?(?:la|lga|lgat)\\s*$");

    private static final Pattern tabPattern = Pattern.compile("^(?:.*\\s+)?(?:t|tb)\\s*$");

    private static final Pattern tabSidePattern = Pattern.compile("^(?:.*\\s+)?(?:ts|tbsd)\\s*$");

    private static final Pattern videoInfoSidePattern = Pattern
            .compile("^(?:.*\\s+)?(?:i|inf|info)\\s*$");

    private static final Pattern spectrumsPattern = Pattern
            .compile("^(?:.*\\s+)?(?:sn|sd|snd|sound)\\s*$"); // see "save"

    private static final Pattern playPausePattern = Pattern
            .compile("^(?:.*\\s+)?(?:p|pp|plps)\\s*$");

    private static final Pattern playPattern = Pattern.compile("^(?:.*\\s+)?(?:pl|ply|play)\\s*$");

    private static final Pattern pausePattern = Pattern
            .compile("^(?:.*\\s+)?(?:ps|pse|pause)\\s*$");

    private static final Pattern backPattern = Pattern.compile("^(?:.*\\s+)?(?:b|bck|back)\\s*$");

    private static final Pattern nextPattern = Pattern.compile("^(?:.*\\s+)?(?:n|nxt|next)\\s*$");

    private static final Pattern timePattern = Pattern
            .compile("^(?:.*\\s+)?(?:t|tm|time)\\s*(\\+|\\-|)\\s*(?:(\\d*)\\s*(?:m|:)?\\s*(\\d*)\\s*s?)\\s*$");

    private static final Pattern volPattern = Pattern
            .compile("^(?:.*\\s+)?(?:v|vl|volume)\\s*(\\+|\\-|)\\s*(\\d*)\\s*$");

    private static final Pattern jumpPattern = Pattern
            .compile("^(?:.*\\s+)?(?:j|jmp|jump)\\s*(\\+|\\-|)\\s*(\\d*)\\s*$");

    private static final Pattern saveFilePattern = Pattern
            .compile("^(?:.*\\s+)?(?:s|sf|svfl|savefile)\\s*$");

    private static final Pattern saveImagePattern = Pattern
            .compile("^(?:.*\\s+)?(?:si|svim|svig|svimg|saveimage)\\s*$");

    private static final Pattern windowScreenPattern = Pattern
            .compile("^(?:.*\\s+)?(?:wm|wnm|wdm|wndm|windowm)\\s*$");

    private static final Pattern windowScreenMinPattern = Pattern
            .compile("^(?:.*\\s+)?(?:wmn|wnmn|wdmn|wndmn|windowmin)\\s*$");

    private static final Pattern windowScreenMaxPattern = Pattern
            .compile("^(?:.*\\s+)?(?:wmx|wnmx|wdmx|wndmx|windowmax)\\s*$");

    private static final Pattern windowFrontPattern = Pattern
            .compile("^(?:.*\\s+)?(?:wf|wnfr|wdfr|wndfrnt)\\s*$");

    private static final Pattern windowBackPattern = Pattern
            .compile("^(?:.*\\s+)?(?:wb|wnbk|wdbk|wndbck)\\s*$");

    private final StringBuilder inputs = new StringBuilder();

    private long lastTime = Long.MIN_VALUE;

    private final IAdapter adapter;

    private final boolean consume;

    public ShortcutHandler(IAdapter adapter) {
        this(adapter, false);
    }

    public ShortcutHandler(IAdapter adapter, boolean consume) {
        this.adapter = adapter;
        this.consume = consume;
    }

    @Override
    public void handle(KeyEvent event) {
        String str = event.getCharacter();
        char ch = str.isEmpty() ? ' ' : str.toLowerCase().charAt(0);
        handle(ch);
        if (consume) {
            event.consume();
        }
    }

    private void handle(char ch) {
        if ((ch == ';') || (ch == '\n') || (ch == '\r')) {
            parse: {
                String in = inputs.toString().trim();
                if (in.isEmpty()) {
                    break parse;
                }
                if (directoryPattern.matcher(in).find()) {
                    adapter.controlDirectoryChooser();
                    break parse;
                }
                if (filePattern.matcher(in).find()) {
                    adapter.controlFileChooser();
                    break parse;
                }
                if (pathPattern.matcher(in).find()) {
                    adapter.focusPathText();
                    break parse;
                }
                if (logPattern.matcher(in).find()) {
                    adapter.controlLog();
                    break parse;
                }
                if (logSnapPattern.matcher(in).find()) {
                    adapter.controlLogSnap();
                    break parse;
                }
                if (logFrontPattern.matcher(in).find()) {
                    adapter.controlLogFront();
                    break parse;
                }
                if (logBackPattern.matcher(in).find()) {
                    adapter.controlLogBack();
                    break parse;
                }
                if (logAutoPattern.matcher(in).find()) {
                    adapter.controlLogAuto();
                    break parse;
                }
                if (tabPattern.matcher(in).find()) {
                    adapter.controlTab();
                    break parse;
                }
                if (tabSidePattern.matcher(in).find()) {
                    adapter.controlTabSide();
                    break parse;
                }
                if (videoInfoSidePattern.matcher(in).find()) {
                    adapter.controlVideoInfoSide();
                    break parse;
                }
                if (spectrumsPattern.matcher(in).find()) {
                    adapter.controlSpectrums();
                    break parse;
                }
                if (playPausePattern.matcher(in).find()) {
                    adapter.controlPlayPause();
                    break parse;
                }
                if (playPattern.matcher(in).find()) {
                    adapter.controlPlay();
                    break parse;
                }
                if (pausePattern.matcher(in).find()) {
                    adapter.controlPause();
                    break parse;
                }
                if (backPattern.matcher(in).find()) {
                    adapter.controlBack();
                    break parse;
                }
                if (nextPattern.matcher(in).find()) {
                    adapter.controlNext();
                    break parse;
                }
                {
                    Matcher matcher = timePattern.matcher(in);
                    if (matcher.find()) {
                        long minutePart;
                        long secondPart;
                        String mg1 = matcher.group(1);
                        String mg2 = matcher.group(2);
                        String mg3 = matcher.group(3);
                        int mg2i = mg2.isEmpty() ? 0 : ((9 < mg2.length()) ? Integer.MAX_VALUE
                                : Integer.parseInt(mg2));
                        int mg3i = mg3.isEmpty() ? 0 : ((9 < mg3.length()) ? Integer.MAX_VALUE
                                : Integer.parseInt(mg3));
                        if (mg2.isEmpty() && mg3.isEmpty()) {
                            minutePart = 0;
                            secondPart = 0;
                        } else if (mg2.isEmpty()) {
                            minutePart = mg3i / 100;
                            secondPart = mg3i % 100;
                        } else if (mg3.isEmpty()) {
                            minutePart = mg2i / 100;
                            secondPart = mg2i % 100;
                        } else {
                            minutePart = mg2i;
                            secondPart = mg3i;
                        }
                        long value = (minutePart * 60) + secondPart;
                        if (mg1.equals("+")) {
                            adapter.controlTimePlus(value);
                        } else if (mg1.equals("-")) {
                            adapter.controlTimeMinus(value);
                        } else {
                            adapter.controlTime(value);
                        }
                        break parse;
                    }
                }
                {
                    Matcher matcher = volPattern.matcher(in);
                    if (matcher.find()) {
                        String mg1 = matcher.group(1);
                        String mg2 = matcher.group(2);
                        int value = mg2.isEmpty() ? 0 : ((9 < mg2.length()) ? Integer.MAX_VALUE
                                : Integer.parseInt(mg2));
                        if (mg1.equals("+")) {
                            adapter.controlVolumePlus(value);
                        } else if (mg1.equals("-")) {
                            adapter.controlVolumeMinus(value);
                        } else {
                            adapter.controlVolume(value);
                        }
                        break parse;
                    }
                }
                {
                    Matcher matcher = jumpPattern.matcher(in);
                    if (matcher.find()) {
                        String mg1 = matcher.group(1);
                        String mg2 = matcher.group(2);
                        int value = mg2.isEmpty() ? 0 : ((9 < mg2.length()) ? Integer.MAX_VALUE
                                : Integer.parseInt(mg2));
                        if (mg1.equals("+")) {
                            adapter.controlJumpPlus(value);
                        } else if (mg1.equals("-")) {
                            adapter.controlJumpMinus(value);
                        } else {
                            adapter.controlJump(value);
                        }
                        break parse;
                    }
                }
                if (saveFilePattern.matcher(in).find()) {
                    adapter.controlSaveFile();
                    break parse;
                }
                if (saveImagePattern.matcher(in).find()) {
                    adapter.controlSaveImage();
                    break parse;
                }
                if (windowScreenPattern.matcher(in).find()) {
                    adapter.controlWindowScreen();
                    break parse;
                }
                if (windowScreenMinPattern.matcher(in).find()) {
                    adapter.controlWindowScreenMin();
                    break parse;
                }
                if (windowScreenMaxPattern.matcher(in).find()) {
                    adapter.controlWindowScreenMax();
                    break parse;
                }
                if (windowFrontPattern.matcher(in).find()) {
                    adapter.controlWindowFront();
                    break parse;
                }
                if (windowBackPattern.matcher(in).find()) {
                    adapter.controlWindowBack();
                    break parse;
                }
            }
            inputs.setLength(0);
        } else {
            long current = System.currentTimeMillis();
            if ((ch == '\b') || (ch == '@') || (ch == '`')) {
                inputs.setLength(0);
            } else {
                if ((lastTime + 30_000) < current) {
                    inputs.append(' ');
                }
                inputs.append(ch);
                if (100 < inputs.length()) {
                    inputs.delete(0, 50);
                }
            }
            lastTime = current;
        }
    }
}
