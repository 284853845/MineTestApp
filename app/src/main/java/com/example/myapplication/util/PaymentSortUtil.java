package com.example.myapplication.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缴费分组排序工具。
 *
 * <p>排序规则：
 * <ol>
 *     <li>欠费金额大于 0 的数据排在前面，未欠费数据排在后面。</li>
 *     <li>如果当前分组内任意一条数据的排序序号大于 0，只做欠费/未欠费排序。</li>
 *     <li>如果当前分组内每条数据的排序序号都小于等于 0，欠费区和未欠费区内部都按缴费类型：水、电、气、话、广排序。</li>
 *     <li>其他情况保持原列表中的相对顺序。</li>
 * </ol>
 */
public final class PaymentSortUtil {

    public static final String TYPE_WATER = "水";
    public static final String TYPE_ELECTRIC = "电";
    public static final String TYPE_GAS = "气";
    public static final String TYPE_PHONE = "话";
    public static final String TYPE_BROADBAND = "广";

    private static final int UNKNOWN_TYPE_RANK = 999;

    private static final Map<String, Integer> TYPE_RANK_MAP = new HashMap<>();

    static {
        List<String> typeOrder = Arrays.asList(
                TYPE_WATER,
                TYPE_ELECTRIC,
                TYPE_GAS,
                TYPE_PHONE,
                TYPE_BROADBAND
        );
        for (int i = 0; i < typeOrder.size(); i++) {
            TYPE_RANK_MAP.put(typeOrder.get(i), i);
        }
    }

    private PaymentSortUtil() {
    }

    /**
     * 对分组列表中的子数组数据进行原地排序，分组本身的顺序不变。
     */
    public static void sortGroups(List<PaymentGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        for (PaymentGroup group : groups) {
            if (group == null) {
                continue;
            }
            sortItems(group.getItems());
        }
    }

    /**
     * 对单个分组的缴费子数组进行原地排序。
     */
    public static void sortItems(List<PaymentItem> items) {
        if (items == null || items.size() <= 1) {
            return;
        }
        Collections.sort(items, itemComparator(!hasValidSortNo(items)));
    }

    /**
     * 返回一个排好序的新列表，不修改原列表。
     */
    public static List<PaymentItem> sortedItems(List<PaymentItem> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        List<PaymentItem> result = new ArrayList<>(items);
        sortItems(result);
        return result;
    }

    private static Comparator<PaymentItem> itemComparator(final boolean sortByPaymentType) {
        return new Comparator<PaymentItem>() {
            @Override
            public int compare(PaymentItem left, PaymentItem right) {
                if (left == right) {
                    return 0;
                }
                if (left == null) {
                    return 1;
                }
                if (right == null) {
                    return -1;
                }

                int debtCompare = Boolean.compare(right.hasArrears(), left.hasArrears());
                if (debtCompare != 0) {
                    return debtCompare;
                }

                if (sortByPaymentType) {
                    return Integer.compare(typeRank(left.getPaymentType()), typeRank(right.getPaymentType()));
                }

                return 0;
            }
        };
    }

    private static boolean hasValidSortNo(List<PaymentItem> items) {
        for (PaymentItem item : items) {
            if (item != null && item.hasValidSortNo()) {
                return true;
            }
        }
        return false;
    }

    private static int typeRank(String paymentType) {
        Integer rank = TYPE_RANK_MAP.get(paymentType);
        return rank == null ? UNKNOWN_TYPE_RANK : rank;
    }

    public static final class PaymentGroup {
        private String groupName;
        private String groupId;
        private List<PaymentItem> items;

        public PaymentGroup(String groupName, String groupId, List<PaymentItem> items) {
            this.groupName = groupName;
            this.groupId = groupId;
            this.items = items;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public List<PaymentItem> getItems() {
            return items;
        }

        public void setItems(List<PaymentItem> items) {
            this.items = items;
        }
    }

    public static final class PaymentItem {
        private String paymentNo;
        private String paymentName;
        private int sortNo;
        private String paymentType;
        private BigDecimal arrearsAmount;

        public PaymentItem(String paymentNo,
                           String paymentName,
                           int sortNo,
                           String paymentType,
                           BigDecimal arrearsAmount) {
            this.paymentNo = paymentNo;
            this.paymentName = paymentName;
            this.sortNo = sortNo;
            this.paymentType = paymentType;
            this.arrearsAmount = arrearsAmount;
        }

        public static PaymentItem of(String paymentNo,
                                     String paymentName,
                                     int sortNo,
                                     String paymentType,
                                     String arrearsAmount) {
            return new PaymentItem(
                    paymentNo,
                    paymentName,
                    sortNo,
                    paymentType,
                    new BigDecimal(arrearsAmount)
            );
        }

        public boolean hasArrears() {
            return arrearsAmount != null && arrearsAmount.compareTo(BigDecimal.ZERO) > 0;
        }

        public boolean hasValidSortNo() {
            return sortNo > 0;
        }

        public String getPaymentNo() {
            return paymentNo;
        }

        public void setPaymentNo(String paymentNo) {
            this.paymentNo = paymentNo;
        }

        public String getPaymentName() {
            return paymentName;
        }

        public void setPaymentName(String paymentName) {
            this.paymentName = paymentName;
        }

        public int getSortNo() {
            return sortNo;
        }

        public void setSortNo(int sortNo) {
            this.sortNo = sortNo;
        }

        public String getPaymentType() {
            return paymentType;
        }

        public void setPaymentType(String paymentType) {
            this.paymentType = paymentType;
        }

        public BigDecimal getArrearsAmount() {
            return arrearsAmount;
        }

        public void setArrearsAmount(BigDecimal arrearsAmount) {
            this.arrearsAmount = arrearsAmount;
        }

        @Override
        public String toString() {
            return "PaymentItem{"
                    + "paymentNo='" + paymentNo + '\''
                    + ", paymentName='" + paymentName + '\''
                    + ", sortNo=" + sortNo
                    + ", paymentType='" + paymentType + '\''
                    + ", arrearsAmount=" + arrearsAmount
                    + '}';
        }
    }
}
