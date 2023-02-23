package com.owner.temp.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.*;

/**
 * LotteryGenerateTest
 *
 * @author XSK
 * @date 2022/10/24 12:31
 */
public class LotteryGenerateUtils {
    /**
     * 生成器
     */
    public static final TiYuLotteryGenerate tiYuLotteryGenerate = new TiYuLotteryGenerate();
    /**
     * 生成器
     */
    public static final FuLiLotteryGenerate fuLiLotteryGenerate = new FuLiLotteryGenerate();

    public static String randomFuLiLottery() {
        return fuLiLotteryGenerate.generate();
    }

    public static String randomTiYuLottery() {
        return tiYuLotteryGenerate.generate();
    }

    public static String randomFuLiLotteryUntil(String numStr) {
        return fuLiLotteryGenerate.nextNum(numStr);
    }

    public static String randomTiYuLotteryUntil(String numStr) {
        return tiYuLotteryGenerate.nextNum(numStr);
    }

    public static void main(String[] args) {

        TiYuLotteryGenerate tiYuLotteryGenerate = new TiYuLotteryGenerate();
        FuLiLotteryGenerate fuLiLotteryGenerate = new FuLiLotteryGenerate();
        // 打印最大概率
//        tiYuLotteryGenerate.printRate();

        // 测试生成指定号码
//        System.out.println("final count : " + fuLiLotteryGenerate
//                .generateUntil("12 17 22 27 30 31 02"));
        System.out.println("final count : " + fuLiLotteryGenerate
                .generateUntil("02 22 26 29 32 33 14"));
        // 随机生成一次
//        System.out.println("generate Fu li Lottery: " + fuLiLotteryGenerate.generate());
//        System.out.println("generate Ti Yu Lottery: " + tiYuLotteryGenerate.generate());

    }

    static class TiYuLotteryGenerate extends LotteryGenerate {
        public static final String TI_YU_OPEN_URL = "https://webapi.sporttery.cn/gateway/lottery/getHistoryPageListV1.qry?gameNo=85&provinceId=0&isVerify=1";

        public static final int LAST_OPEN_COUNT = 1000;

        TiYuLotteryGenerate() {
            super(new LotteryRange(1, 35), new LotteryRange(1, 12)
                    , 5, 2);
        }

        @Override
        public void printRate() {
            int avgCount = 100;
            String result = HttpUtils.sendGet(TI_YU_OPEN_URL + "&termLimits=" + LAST_OPEN_COUNT);
            // 解析获取所有list
            JSONObject jsonObject = JSONObject.parseObject(result);
            JSONObject value = jsonObject.getJSONObject("value");
            JSONArray list = value.getJSONArray("list");
            Map<Integer, Integer> firstTotalCount = new TreeMap<>(Integer::compareTo);
            Map<Integer, Integer> secondTotalCount = new TreeMap<>(Integer::compareTo);
            Map<Integer, Map<Integer, Integer>> firstAvgCountMap = new LinkedHashMap<>();
            Map<Integer, Map<Integer, Integer>> secondAvgCountMap = new LinkedHashMap<>();
            // lotteryDrawResult
            for (int i = 0; i < list.size(); i++) {
                String lotteryNum = list.getJSONObject(i).getString("lotteryDrawResult");
                int[] firstAreaNums = new int[getFirstAreaLength()];
                int[] secondAreaNums = new int[getSecondAreaLength()];
                this.parseBallNums(lotteryNum, firstAreaNums, secondAreaNums);

                //每100次统计
                int countKey = (i / avgCount + 1) * 100;
                // firstArea avg count
                contains(firstAvgCountMap, firstAreaNums, countKey);
                // secondArea avg count
                contains(secondAvgCountMap, secondAreaNums, countKey);

                setIntAryToMap(firstAreaNums, firstTotalCount);
                setIntAryToMap(secondAreaNums, secondTotalCount);
            }
            int[] firstAreaNums = new int[getFirstAreaLength()];
            int[] secondAreaNums = new int[getSecondAreaLength()];

            for (Integer key : firstAvgCountMap.keySet()) {
                Map<Integer, Integer> map = firstAvgCountMap.get(key);
                System.out.println(key + " first area begin");
                ArrayList<Map.Entry<Integer, Integer>> entries = new ArrayList<>(map.entrySet());
                entries.sort((o1, o2) -> o1.getValue().compareTo(o2.getKey()));
                calcRate(entries, null, avgCount);
            }
            for (Integer key : secondAvgCountMap.keySet()) {
                System.out.println(key + " second area begin");
                Map<Integer, Integer> map = secondAvgCountMap.get(key);
                ArrayList<Map.Entry<Integer, Integer>> entries = new ArrayList<>(map.entrySet());
                entries.sort((o1, o2) -> o1.getValue().compareTo(o2.getKey()));
                calcRate(entries, null, avgCount);
            }

            List<Map.Entry<Integer, Integer>> entryF = new ArrayList<>(firstTotalCount.entrySet());
            entryF.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
            System.out.println("first area begin...");
            calcRate(entryF, firstAreaNums, LAST_OPEN_COUNT);

            List<Map.Entry<Integer, Integer>> secondEntry = new ArrayList<>(secondTotalCount.entrySet());
            secondEntry.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
            System.out.println("second area begin...");
            calcRate(secondEntry, secondAreaNums, LAST_OPEN_COUNT);

            System.out.println("Max rate: " + toNumString(firstAreaNums, secondAreaNums));
        }

        /**
         * 包含指定key
         *
         * @param secondAvgCountMap secondAvgCountMap
         * @param secondAreaNums    secondAreaNums
         * @param countKey          countKey
         */
        private void contains(Map<Integer, Map<Integer, Integer>> secondAvgCountMap, int[] secondAreaNums, int countKey) {
            if (secondAvgCountMap.containsKey(countKey)) {
                Map<Integer, Integer> integerMap = secondAvgCountMap.get(countKey);
                setIntAryToMap(secondAreaNums, integerMap);
            } else {
                secondAvgCountMap.put(countKey, new HashMap<>());
                Map<Integer, Integer> integerMap = secondAvgCountMap.get(countKey);
                setIntAryToMap(secondAreaNums, integerMap);
            }
        }

        private void setIntAryToMap(int[] nums, Map<Integer, Integer> countMap) {
            for (int num : nums) {
                int count = countMap.get(num) == null ? 1 : countMap.get(num) + 1;
                countMap.put(num, count);
            }
        }

        private void calcRate(List<Map.Entry<Integer, Integer>> entryF, int[] firstAreaNums, int totalCount) {
            int countF = 0;
            for (Map.Entry<Integer, Integer> num : entryF) {
                if (firstAreaNums != null) {
                    if (countF < firstAreaNums.length) {
                        firstAreaNums[countF] = num.getKey();
                    }
                }
                System.out.println("[" + num.getKey() + "] count: " + num.getValue()
                        + " rate: " + num.getValue() / (totalCount + 0.0));
                countF++;
            }
        }
    }

    static class FuLiLotteryGenerate extends LotteryGenerate {
        private static final String URL
                = "http://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=ssq";

        FuLiLotteryGenerate() {
            super(new LotteryRange(1, 33), new LotteryRange(1, 16)
                    , 6, 1);
        }

        public String lastNum() {
            int avgCount = 1;
            String url = URL + "&issueCount=" + avgCount;
            String result = HttpUtils.sendGet(url);
            // 解析获取所有list
            JSONObject jsonObject = JSONObject.parseObject(result);
            JSONArray list = jsonObject.getJSONArray("result");
            JSONObject resultJson = list.getJSONObject(0);
            String red = resultJson.getString("red").replace(",", " ");
            return red + " " + resultJson.getString("blue");
        }

        @Override
        public void printRate() {
            super.printRate();
        }
    }

    static class LotteryGenerate implements Generator {
        private final LotteryRange blueBallRange;
        private final LotteryRange yellowBallRange;
        private final int firstAreaLength;
        private final int secondAreaLength;
        private int[] first;
        private int[] second;
        private int totalCount = 0;
        private final int[] numOfEqCount;

        LotteryGenerate(LotteryRange firstRange, LotteryRange secondRange
                , int firstAreaLength, int secondAreaLength) {
            blueBallRange = firstRange;
            yellowBallRange = secondRange;
            this.firstAreaLength = firstAreaLength;
            this.secondAreaLength = secondAreaLength;
            first = new int[firstAreaLength];
            second = new int[secondAreaLength];
            numOfEqCount = new int[firstAreaLength + secondAreaLength];
        }

        @Override
        public String generate() {
            String result = toNumString(blueNums(), yellowNums());
            reset();
            return result;
        }

        @Override
        public Generator exclude(String nums) {
            int[] numAry = parseBall(nums);
            for (int i = 0; i < firstAreaLength; i++) {
                blueBallRange.exclude(numAry[i]);
            }
            for (int i = firstAreaLength; i < secondAreaLength; i++) {
                yellowBallRange.exclude(numAry[i]);
            }
            return this;
        }

        @Override
        public int generateUntil(String numStr) {
            boolean condition = Boolean.TRUE;
            int[] firstAreaNums = new int[firstAreaLength];
            int[] secondAreaNums = new int[secondAreaLength];
            parseBallNums(numStr, firstAreaNums, secondAreaNums);
            System.out.println("parse and sort: " + toNumString(firstAreaNums, secondAreaNums));
            int count = 0;
            while (condition) {
                first = sortList(this.blueNums());
                second = sortList(this.yellowNums());
                reset();
                condition = !ballEquals(firstAreaNums, secondAreaNums, first, second);
                count++;
            }
            return count;
        }

        public String nextNum(String numStr) {
            int i = generateUntil(numStr);
            System.out.println("count: " + i);
            first = sortList(this.blueNums());
            second = sortList(this.yellowNums());
            reset();
            return toNumString(first, second);
        }

        @Override
        public void printRate() {
        }

        public void parseBallNums(String numStr, int[] firstAreaNums, int[] secondAreaNums) {
            subBallNums(parseBall(numStr), firstAreaNums, secondAreaNums);
        }

        private void subBallNums(int[] from, int[] firstAry, int[] secondAry) {
            for (int i = 0; i < from.length; i++) {
                if (i < firstAreaLength) {
                    firstAry[i] = from[i];
                } else {
                    secondAry[i - firstAreaLength] = from[i];
                }
            }
            sortList(firstAry);
            sortList(secondAry);
        }

        private boolean ballEquals(int[] firstAreaNumsFrom, int[] secondAreaNumsFrom
                , int[] firstAreaNumsTo, int[] secondAreaNumsTo) {
            boolean isTure = true;
            int count = 0;
            for (int i = 0; i < firstAreaNumsFrom.length; i++) {
                if (firstAreaNumsFrom[i] != firstAreaNumsTo[i]) {
                    isTure = false;
                } else {
                    count++;
                }
                totalCount++;
            }
            for (int i = 0; i < secondAreaNumsFrom.length; i++) {
                if (secondAreaNumsFrom[i] != secondAreaNumsTo[i]) {
                    isTure = false;
                } else {
                    count++;
                }
                totalCount++;
            }
            if (count > 5) {
                System.out.println(toNumString(firstAreaNumsTo, secondAreaNumsTo));
            }
//            printRate(count);
            return isTure;
        }

        /**
         * 打印概率
         *
         * @param index index
         */
        private void printRate(int index) {
            if (index == 0) {
                return;
            }
            System.out.print("total: " + totalCount + " ");
            index -= 1;
            numOfEqCount[index] += 1;
            for (int i = 0; i < numOfEqCount.length; i++) {
                int present = numOfEqCount[i] / totalCount * 100;
                System.out.print((i + 1) + "[rate: " + present + " % count: " + numOfEqCount[i] + "], ");
            }
            System.out.println();
        }

        private int[] parseBall(String nums) {
            int[] numAry = new int[firstAreaLength + secondAreaLength];
            String[] numStr = nums.split(" ");
            for (int i = 0; i < numAry.length; i++) {
                numAry[i] = Integer.parseInt(numStr[i]);
            }
            return numAry;
        }

        public String toNumString(int[] blue, int[] yellow) {
            return genNumString(blue)
                    + " " + genNumString(yellow);
        }

        private int[] blueNums() {
            return getNums(blueBallRange, first, firstAreaLength);
        }

        private int[] yellowNums() {
            return getNums(yellowBallRange, second, secondAreaLength);
        }

        private void reset() {
            blueBallRange.clear();
            yellowBallRange.clear();
        }

        public String genNumString(int[] numberList) {
            StringBuilder numStr = new StringBuilder();
            boolean isFirst = true;
            for (Integer integer : numberList) {
                if (!isFirst) {
                    numStr.append(" ");
                } else {
                    isFirst = false;
                }
                numStr.append(integer < 10 ? "0" + integer : integer);
            }
            return numStr.toString();
        }

        private int[] getNums(LotteryRange range, int[] ary, int count) {
            for (int i = 0; i < count; i++) {
                ary[i] = range.randomNum();
            }
            return sortList(ary);
        }

        private int[] sortList(int[] list) {
            Arrays.sort(list);
            return list;
        }

        public int getFirstAreaLength() {
            return firstAreaLength;
        }

        public int getSecondAreaLength() {
            return secondAreaLength;
        }
    }

    /**
     * 号码生成接口
     */
    interface Generator {
        /**
         * 生成号码
         *
         * @return String
         */
        String generate();

        /**
         * 排除指定号码
         *
         * @param nums nums
         * @return Generator
         */
        Generator exclude(String nums);

        /**
         * 生成号码直至等于 nums号码
         *
         * @param nums nums
         * @return 生成次数
         */
        int generateUntil(String nums);

        /**
         * 出现概率
         */
        void printRate();
    }

    static class LotteryRange {
        private int begin;
        private int end;
        private final Set<Integer> excludeNum;
        private final Set<Integer> repeatNum;
        private final Random random;

        LotteryRange(int begin, int end) {
            this.begin = begin;
            this.end = end;
            excludeNum = new HashSet<>();
            repeatNum = new HashSet<>();
            random = new Random();
        }

        public int randomNum() {
            boolean condition = Boolean.TRUE;
            while (condition) {
                int num;
                if (!excludeNum.contains(num = nextRandomNum()) && !repeatNum.contains(num)) {
                    excludeRepeat(num);
                    return num;
                }
                condition = excludeNum.size() != getEnd() - getBegin();
            }
            throw new IllegalStateException("full size: " + excludeNum.toString());
        }

        public void clear() {
            repeatNum.clear();
        }

        private void excludeRepeat(int number) {
            repeatNum.add(number);
        }

        public void exclude(int number) {
            excludeNum.add(number);
        }

        private int nextRandomNum() {
            return random.nextInt(end - begin) + begin;
        }

        public int getBegin() {
            return begin;
        }

        public void setBegin(int begin) {
            this.begin = begin;
        }

        public int getEnd() {
            return end;
        }

        public void setEnd(int end) {
            this.end = end;
        }
    }
}
