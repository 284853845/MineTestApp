package com.example.myapplication.module.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按后端返回的 key 实例化模块。新增模块时在这里注册一行 key -> 构造器即可,
 * 页面本身不需要知道有哪些模块,真正做到"后端配置驱动"。
 */
public class ModuleFactory {

    /** 模块构造器 */
    public interface Creator {
        Module create();
    }

    private final Map<String, Creator> creators = new HashMap<>();

    public ModuleFactory register(String key, Creator creator) {
        creators.put(key, creator);
        return this;
    }

    /** 按一组有序 key 创建模块;未知 key 会被跳过(后端新增模块、旧客户端兼容) */
    public List<Module> create(List<String> orderedKeys) {
        List<Module> modules = new ArrayList<>();
        if (orderedKeys == null) {
            return modules;
        }
        for (String key : orderedKeys) {
            Creator creator = creators.get(key);
            if (creator != null) {
                modules.add(creator.create());
            }
        }
        return modules;
    }
}
