package com.example.myapplication.list.model;

import com.example.myapplication.list.core.ModuleItem;

import java.util.List;

/** 一行多个统计数字的模块 */
public class StatsModuleItem implements ModuleItem {

    public static class Stat {
        public final String value;
        public final String label;

        public Stat(String value, String label) {
            this.value = value;
            this.label = label;
        }
    }

    public final List<Stat> stats;

    public StatsModuleItem(List<Stat> stats) {
        this.stats = stats;
    }
}
