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

        void controlVideoInfoWidth(double width);

        void controlVideoInfoWidthPlus(double width);

        void controlVideoInfoWidthMinus(double width);

        void controlVideoInfoHeight(double height);

        void controlVideoInfoHeightPlus(double height);

        void controlVideoInfoHeightMinus(double height);

        void controlSpectrums();

        void controlTimesVolumes();

        void controlPlayPause();

        void controlPlay();

        void controlPause();

        void controlRepeat(NextMode next);

        void controlDirection(OtherDirection direction);

        void controlHead();

        void controlBack();

        void controlNext();

        void controlTime(long seconds);

        void controlTimePlus(long seconds);

        void controlTimeMinus(long seconds);

        void controlTimeTail(long seconds);

        void controlMute();

        void controlVolume(int volume);

        void controlVolumePlus(int volume);

        void controlVolumeMinus(int volume);

        void controlVolumeTail(int volume);

        void controlJump(int value);

        void controlJumpPlus(int value);

        void controlJumpMinus(int value);

        void controlRemove(int value);

        void controlRemovePlus(int value);

        void controlRemoveMinus(int value);

        void controlSaveFile();

        void controlSaveImage();

        void controlSaveSnapshot();

        void controlHideHeader();

        void controlHideFooter();

        void controlWindowScreen();

        void controlWindowScreenMin();

        void controlWindowScreenMax();

        void controlWindowFront();

        void controlWindowBack();

        void controlWindowIconified();

        void controlWindowExit();

        void controlOther(String[] args);
    }

    private static final Pattern directoryPattern = Pattern
            .compile("^(?:.*\\s+)?(?:d|dir|directory)$");

    private static final Pattern filePattern = Pattern.compile("^(?:.*\\s+)?(?:f|fl|file)$");

    private static final Pattern pathPattern = Pattern.compile("^(?:.*\\s+)?(?:u|url)$");

    private static final Pattern logPattern = Pattern.compile("^(?:.*\\s+)?(?:l|lg|log)$");

    private static final Pattern logSnapPattern = Pattern
            .compile("^(?:.*\\s+)?(?:ll|lglg|loglog)$");

    private static final Pattern logFrontPattern = Pattern
            .compile("^(?:.*\\s+)?(?:lf|lgfr|lgfrnt)$");

    private static final Pattern logBackPattern = Pattern.compile("^(?:.*\\s+)?(?:lb|lgbk|lgbck)$");

    private static final Pattern logAutoPattern = Pattern.compile("^(?:.*\\s+)?(?:la|lga|lgat)$");

    private static final Pattern tabPattern = Pattern.compile("^(?:.*\\s+)?(?:t|tb)$");

    private static final Pattern tabSidePattern = Pattern.compile("^(?:.*\\s+)?(?:ts|tbsd)$");

    private static final Pattern videoInfoSidePattern = Pattern
            .compile("^(?:.*\\s+)?(?:i|inf|info)$");

    private static final Pattern videoInfoWidthPattern = Pattern
            .compile("^(?:.*\\s+)?(?:iw|infw|infowidth)\\s*(\\+|\\-|)\\s*(\\d*)$");

    private static final Pattern videoInfoHeightPattern = Pattern
            .compile("^(?:.*\\s+)?(?:ih|infh|infoheight)\\s*(\\+|\\-|)\\s*(\\d*)$");

    // see "save"
    private static final Pattern spectrumsPattern = Pattern
            .compile("^(?:.*\\s+)?(?:sn|sd|snd|sound)$");

    private static final Pattern timesVolumesPattern = Pattern
            .compile("^(?:.*\\s+)?(?:hr|hrzn|hrizon|hrzntl|hrizontal)$");

    private static final Pattern playPausePattern = Pattern.compile("^(?:.*\\s+)?(?:p|pp|plps)$");

    private static final Pattern playPattern = Pattern.compile("^(?:.*\\s+)?(?:pl|ply|play)$");

    private static final Pattern pausePattern = Pattern.compile("^(?:.*\\s+)?(?:ps|pause)$");

    private static final Pattern repeatPattern = Pattern
            .compile("^(?:.*\\s+)?(?:r|rp|rpt|repeat)\\s*(none|same|other|)$");

    private static final Pattern directionPattern = Pattern
            .compile("^(?:.*\\s+)?(?:dr|drct|direction)\\s*(forward|back|)$");

    // see "time volume horizontal"
    private static final Pattern headPattern = Pattern.compile("^(?:.*\\s+)?(?:h|hd|head)$");

    private static final Pattern backPattern = Pattern.compile("^(?:.*\\s+)?(?:b|bck|back)$");

    private static final Pattern nextPattern = Pattern.compile("^(?:.*\\s+)?(?:n|nxt|next)$");

    private static final Pattern timePattern = Pattern
            .compile("^(?:.*\\s+)?(?:t|tm|time)\\s*(\\+|\\-|)\\s*(?:(\\d*)\\s*(?::)?\\s*(\\d*))\\s*(\\-|)$");

    private static final Pattern mutePattern = Pattern.compile("^(?:.*\\s+)?(?:m|mt|mute)$");

    private static final Pattern volPattern = Pattern
            .compile("^(?:.*\\s+)?(?:v|vl|volume)\\s*(\\+|\\-|)\\s*(\\d*)\\s*(\\-|)$");

    private static final Pattern jumpPattern = Pattern
            .compile("^(?:.*\\s+)?(?:j|jmp|jump)\\s*(\\+|\\-|)\\s*(\\d*)$");

    private static final Pattern removePattern = Pattern
            .compile("^(?:.*\\s+)?(?:rm|rmv|remove)\\s*(\\+|\\-|)\\s*(\\d*)$");

    private static final Pattern saveFilePattern = Pattern
            .compile("^(?:.*\\s+)?(?:s|sf|svfl|savefile)$");

    private static final Pattern saveImagePattern = Pattern
            .compile("^(?:.*\\s+)?(?:si|svim|svig|svimg|saveimage)$");

    private static final Pattern saveSnapshotPattern = Pattern
            .compile("^(?:.*\\s+)?(?:ss|svsn|svss|savesnapshot)$");

    private static final Pattern hideHeaderPattern = Pattern
            .compile("^(?:.*\\s+)?(?:wh|wnhd|wndhd|wndhead)$");

    private static final Pattern hideFooterPattern = Pattern
            .compile("^(?:.*\\s+)?(?:wf|wnft|wndft|wndfoot)$");

    private static final Pattern windowScreenPattern = Pattern
            .compile("^(?:.*\\s+)?(?:wm|wnm|wdm|wndm|windowm)$");

    private static final Pattern windowScreenMinPattern = Pattern
            .compile("^(?:.*\\s+)?(?:wmn|wnmn|wdmn|wndmn|windowmin)$");

    private static final Pattern windowScreenMaxPattern = Pattern
            .compile("^(?:.*\\s+)?(?:wmx|wnmx|wdmx|wndmx|windowmax)$");

    private static final Pattern windowFrontPattern = Pattern
            .compile("^(?:.*\\s+)?(?:wf|wnfr|wdfr|wndfrnt)$");

    private static final Pattern windowBackPattern = Pattern
            .compile("^(?:.*\\s+)?(?:wb|wnbk|wdbk|wndbck)$");

    private static final Pattern windowIconifiedPattern = Pattern
            .compile("^(?:.*\\s+)?(?:wi|wnic|wdic|wndicn)$");

    private static final Pattern windowExitPattern = Pattern
            .compile("^(?:.*\\s+)?(?:we|wnex|wdex|wndext)$");

    private static final Pattern otherPattern = Pattern.compile("^(?:.*\\s+)?(?:sudo)\\s*(.*)$");

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
        char ch = ((str == null) || str.isEmpty()) ? ' ' : str.charAt(0);
        handle(ch);
        if (consume) {
            event.consume();
        }
    }

    private void handle(char ch) {
        if ((ch == ';') || (ch == '\n') || (ch == '\r')) {
            parse: {
                String in = inputs.toString().trim().toLowerCase();
                if (in.isEmpty()) {
                    break parse;
                }
                if (12 < in.length()) {
                    // adjust
                    String[] strs = in.split("\\s+");
                    int length = strs.length;
                    // max length "t + 12 m 23 s" => 6 words
                    if (6 < length) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = Math.max(0, length - 6); i < length; i++) {
                            if (0 < sb.length()) {
                                sb.append(' ');
                            }
                            sb.append(strs[i]);
                        }
                        in = sb.toString();
                    }
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
                {
                    Matcher matcher = videoInfoWidthPattern.matcher(in);
                    if (matcher.find()) {
                        String mg1 = matcher.group(1);
                        String mg2 = matcher.group(2);
                        double value = mg2.isEmpty() ? Double.NaN
                                : ((9 < mg2.length()) ? Integer.MAX_VALUE : Double.parseDouble(mg2));
                        if (mg1.equals("+")) {
                            adapter.controlVideoInfoWidthPlus(value);
                        } else if (mg1.equals("-")) {
                            adapter.controlVideoInfoWidthMinus(value);
                        } else {
                            adapter.controlVideoInfoWidth(value);
                        }
                        break parse;
                    }
                }
                {
                    Matcher matcher = videoInfoHeightPattern.matcher(in);
                    if (matcher.find()) {
                        String mg1 = matcher.group(1);
                        String mg2 = matcher.group(2);
                        double value = mg2.isEmpty() ? Double.NaN
                                : ((9 < mg2.length()) ? Integer.MAX_VALUE : Double.parseDouble(mg2));
                        if (mg1.equals("+")) {
                            adapter.controlVideoInfoHeightPlus(value);
                        } else if (mg1.equals("-")) {
                            adapter.controlVideoInfoHeightMinus(value);
                        } else {
                            adapter.controlVideoInfoHeight(value);
                        }
                        break parse;
                    }
                }
                if (spectrumsPattern.matcher(in).find()) {
                    adapter.controlSpectrums();
                    break parse;
                }
                if (timesVolumesPattern.matcher(in).find()) {
                    adapter.controlTimesVolumes();
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
                {
                    Matcher matcher = repeatPattern.matcher(in);
                    if (matcher.find()) {
                        String mg1 = matcher.group(1);
                        NextMode next = null;
                        if (mg1.startsWith("n")) {
                            next = NextMode.NONE;
                        } else if (mg1.startsWith("s")) {
                            next = NextMode.SAME;
                        } else if (mg1.startsWith("o")) {
                            next = NextMode.OTHER;
                        }
                        adapter.controlRepeat(next);
                        break parse;
                    }
                }
                {
                    Matcher matcher = directionPattern.matcher(in);
                    if (matcher.find()) {
                        String mg1 = matcher.group(1);
                        OtherDirection direction = null;
                        if (mg1.startsWith("f")) {
                            direction = OtherDirection.FORWARD;
                        } else if (mg1.startsWith("b")) {
                            direction = OtherDirection.BACK;
                        }
                        adapter.controlDirection(direction);
                        break parse;
                    }
                }
                if (headPattern.matcher(in).find()) {
                    adapter.controlHead();
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
                        String mg4 = matcher.group(4);
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
                        if (mg4.isEmpty()) {
                            if (mg1.equals("+")) {
                                adapter.controlTimePlus(value);
                            } else if (mg1.equals("-")) {
                                adapter.controlTimeMinus(value);
                            } else {
                                adapter.controlTime(value);
                            }
                        } else {
                            if (mg1.isEmpty()) {
                                adapter.controlTimeTail(value);
                            }
                        }
                        break parse;
                    }
                }
                if (mutePattern.matcher(in).find()) {
                    adapter.controlMute();
                    break parse;
                }
                {
                    Matcher matcher = volPattern.matcher(in);
                    if (matcher.find()) {
                        String mg1 = matcher.group(1);
                        String mg2 = matcher.group(2);
                        String mg3 = matcher.group(3);
                        int value = mg2.isEmpty() ? 0 : ((9 < mg2.length()) ? Integer.MAX_VALUE
                                : Integer.parseInt(mg2));
                        if (mg3.isEmpty()) {
                            if (mg1.equals("+")) {
                                adapter.controlVolumePlus(value);
                            } else if (mg1.equals("-")) {
                                adapter.controlVolumeMinus(value);
                            } else {
                                adapter.controlVolume(value);
                            }
                        } else {
                            if (mg1.isEmpty()) {
                                adapter.controlVolumeTail(value);
                            }
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
                {
                    Matcher matcher = removePattern.matcher(in);
                    if (matcher.find()) {
                        String mg1 = matcher.group(1);
                        String mg2 = matcher.group(2);
                        int value = mg2.isEmpty() ? 0 : ((9 < mg2.length()) ? Integer.MAX_VALUE
                                : Integer.parseInt(mg2));
                        if (mg1.equals("+")) {
                            adapter.controlRemovePlus(value);
                        } else if (mg1.equals("-")) {
                            adapter.controlRemoveMinus(value);
                        } else {
                            adapter.controlRemove(value);
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
                if (saveSnapshotPattern.matcher(in).find()) {
                    adapter.controlSaveSnapshot();
                    break parse;
                }
                if (hideHeaderPattern.matcher(in).find()) {
                    adapter.controlHideHeader();
                    break parse;
                }
                if (hideFooterPattern.matcher(in).find()) {
                    adapter.controlHideFooter();
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
                if (windowIconifiedPattern.matcher(in).find()) {
                    adapter.controlWindowIconified();
                    break parse;
                }
                if (windowExitPattern.matcher(in).find()) {
                    adapter.controlWindowExit();
                    break parse;
                }
                {
                    Matcher matcher = otherPattern.matcher(in);
                    if (matcher.find()) {
                        String str = matcher.group(1);
                        adapter.controlOther(str.split("\\s+"));
                        break parse;
                    }
                }
            }
            inputs.setLength(0);
        } else {
            long current = System.currentTimeMillis();
            if ((ch == '\b') || (ch == '@') || (ch == '`')) {
                inputs.setLength(0);
            } else {
                if ((lastTime + 30_000) < current) {
                    inputs.setLength(0);
                } else {
                    if (100 < inputs.length()) {
                        inputs.delete(0, 50);
                    }
                }
                inputs.append(ch);
            }
            lastTime = current;
        }
    }

    public static class SimpleAdapter implements IAdapter {

        @Override
        public void controlDirectoryChooser() {
        }

        @Override
        public void controlFileChooser() {
        }

        @Override
        public void focusPathText() {
        }

        @Override
        public void controlLog() {
        }

        @Override
        public void controlLogSnap() {
        }

        @Override
        public void controlLogFront() {
        }

        @Override
        public void controlLogBack() {
        }

        @Override
        public void controlLogAuto() {
        }

        @Override
        public void controlTab() {
        }

        @Override
        public void controlTabSide() {
        }

        @Override
        public void controlVideoInfoSide() {
        }

        @Override
        public void controlVideoInfoWidth(double width) {
        }

        @Override
        public void controlVideoInfoWidthPlus(double width) {
        }

        @Override
        public void controlVideoInfoWidthMinus(double width) {
        }

        @Override
        public void controlVideoInfoHeight(double height) {
        }

        @Override
        public void controlVideoInfoHeightPlus(double height) {
        }

        @Override
        public void controlVideoInfoHeightMinus(double height) {
        }

        @Override
        public void controlSpectrums() {
        }

        @Override
        public void controlTimesVolumes() {
        }

        @Override
        public void controlPlayPause() {
        }

        @Override
        public void controlPlay() {
        }

        @Override
        public void controlPause() {
        }

        @Override
        public void controlRepeat(NextMode next) {
        }

        @Override
        public void controlDirection(OtherDirection direction) {
        }

        @Override
        public void controlHead() {
        }

        @Override
        public void controlBack() {
        }

        @Override
        public void controlNext() {
        }

        @Override
        public void controlTime(long seconds) {
        }

        @Override
        public void controlTimePlus(long seconds) {
        }

        @Override
        public void controlTimeMinus(long seconds) {
        }

        @Override
        public void controlTimeTail(long seconds) {
        }

        @Override
        public void controlMute() {
        }

        @Override
        public void controlVolume(int volume) {
        }

        @Override
        public void controlVolumePlus(int volume) {
        }

        @Override
        public void controlVolumeMinus(int volume) {
        }

        @Override
        public void controlVolumeTail(int volume) {
        }

        @Override
        public void controlJump(int value) {
        }

        @Override
        public void controlJumpPlus(int value) {
        }

        @Override
        public void controlJumpMinus(int value) {
        }

        @Override
        public void controlRemove(int value) {
        }

        @Override
        public void controlRemovePlus(int value) {
        }

        @Override
        public void controlRemoveMinus(int value) {
        }

        @Override
        public void controlSaveFile() {
        }

        @Override
        public void controlSaveImage() {
        }

        @Override
        public void controlSaveSnapshot() {
        }

        @Override
        public void controlHideHeader() {
        }

        @Override
        public void controlHideFooter() {
        }

        @Override
        public void controlWindowScreen() {
        }

        @Override
        public void controlWindowScreenMin() {
        }

        @Override
        public void controlWindowScreenMax() {
        }

        @Override
        public void controlWindowFront() {
        }

        @Override
        public void controlWindowBack() {
        }

        @Override
        public void controlWindowIconified() {
        }

        @Override
        public void controlWindowExit() {
        }

        @Override
        public void controlOther(String[] args) {
        }
    }
}
