package com.example.myapplication;

import com.example.myapplication.util.PaymentSortUtil;
import com.example.myapplication.util.PaymentSortUtil.PaymentGroup;
import com.example.myapplication.util.PaymentSortUtil.PaymentItem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PaymentSortUtilDemoTest {

    @Test
    public void demoSortGroups() {
        List<PaymentGroup> groups = new ArrayList<>();
        groups.add(new PaymentGroup("家庭缴费", "G001", new ArrayList<>(Arrays.asList(
                PaymentItem.of("P001", "家庭宽带", 2, PaymentSortUtil.TYPE_BROADBAND, "0"),
                PaymentItem.of("P002", "燃气费", 1, PaymentSortUtil.TYPE_GAS, "18.50"),
                PaymentItem.of("P003", "水费", 3, PaymentSortUtil.TYPE_WATER, "32.60"),
                PaymentItem.of("P004", "电费", 0, PaymentSortUtil.TYPE_ELECTRIC, "86.20"),
                PaymentItem.of("P005", "手机话费", 1, PaymentSortUtil.TYPE_PHONE, "0"),
                PaymentItem.of("P006", "备用水费", 1, PaymentSortUtil.TYPE_WATER, "0")
        ))));
        groups.add(new PaymentGroup("父母缴费", "G002", new ArrayList<>(Arrays.asList(
                PaymentItem.of("P101", "父母话费", 4, PaymentSortUtil.TYPE_PHONE, "11.00"),
                PaymentItem.of("P102", "父母水费", 2, PaymentSortUtil.TYPE_WATER, "0"),
                PaymentItem.of("P103", "父母电费", 1, PaymentSortUtil.TYPE_ELECTRIC, "0"),
                PaymentItem.of("P104", "父母燃气", 3, PaymentSortUtil.TYPE_GAS, "5.00"),
                PaymentItem.of("P105", "父母宽带", 5, PaymentSortUtil.TYPE_BROADBAND, "20.00")
        ))));
        groups.add(new PaymentGroup("全部无排序号", "G003", new ArrayList<>(Arrays.asList(
                PaymentItem.of("P201", "无序宽带", 0, PaymentSortUtil.TYPE_BROADBAND, "0"),
                PaymentItem.of("P202", "无序燃气", 0, PaymentSortUtil.TYPE_GAS, "18.50"),
                PaymentItem.of("P203", "无序水费", 0, PaymentSortUtil.TYPE_WATER, "32.60"),
                PaymentItem.of("P204", "无序电费", 0, PaymentSortUtil.TYPE_ELECTRIC, "86.20"),
                PaymentItem.of("P205", "无序话费", 0, PaymentSortUtil.TYPE_PHONE, "0"),
                PaymentItem.of("P206", "无序备用水费", 0, PaymentSortUtil.TYPE_WATER, "0")
        ))));

        printGroups("排序前", groups);

        PaymentSortUtil.sortGroups(groups);

        printGroups("排序后", groups);

        assertEquals(Arrays.asList("P002", "P003", "P004", "P001", "P005", "P006"),
                paymentNos(groups.get(0).getItems()));
        assertEquals(Arrays.asList("P101", "P104", "P105", "P102", "P103"),
                paymentNos(groups.get(1).getItems()));
        assertEquals(Arrays.asList("P203", "P204", "P202", "P206", "P205", "P201"),
                paymentNos(groups.get(2).getItems()));
    }

    private static List<String> paymentNos(List<PaymentItem> items) {
        List<String> result = new ArrayList<>();
        for (PaymentItem item : items) {
            result.add(item.getPaymentNo());
        }
        return result;
    }

    private static void printGroups(String title, List<PaymentGroup> groups) {
        System.out.println();
        System.out.println("==== " + title + " ====");
        for (PaymentGroup group : groups) {
            System.out.println(group.getGroupName() + "(" + group.getGroupId() + ")");
            for (PaymentItem item : group.getItems()) {
                System.out.println("  " + item);
            }
        }
    }
}
