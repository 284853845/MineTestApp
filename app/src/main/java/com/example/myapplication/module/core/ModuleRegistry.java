package com.example.myapplication.module.core;

import com.example.myapplication.module.biz.home.AssetOverviewModule;
import com.example.myapplication.module.biz.home.BadgeGridModule;
import com.example.myapplication.module.biz.home.BannerModule;
import com.example.myapplication.module.biz.home.HomeLoopBannerModule;
import com.example.myapplication.module.biz.home.LatestSmsModule;
import com.example.myapplication.module.biz.home.QuickEntryModule;
import com.example.myapplication.module.biz.home.TransactionListModule;
import com.example.myapplication.module.biz.life.BillCardModule;
import com.example.myapplication.module.biz.life.CouponModule;
import com.example.myapplication.module.biz.life.LifeServiceModule;
import com.example.myapplication.module.biz.loan.CreditLimitModule;
import com.example.myapplication.module.biz.loan.LoanProductModule;
import com.example.myapplication.module.biz.loan.RepayReminderModule;
import com.example.myapplication.module.biz.profile.AssetMiniModule;
import com.example.myapplication.module.biz.profile.ProfileHeaderModule;
import com.example.myapplication.module.biz.profile.SettingListModule;
import com.example.myapplication.module.biz.wealth.FundRecommendModule;
import com.example.myapplication.module.biz.wealth.HoldingSummaryModule;
import com.example.myapplication.module.biz.wealth.PromotionModule;

/**
 * 模块注册中心。把所有业务模块的 key -> 构造器集中注册成一个 ModuleFactory。
 *
 * 新增一个模块时,唯一需要改的全局位置就是这里加一行 register;
 * 页面/Manager 只按后端返回的 key 取用,不感知具体模块类型。
 */
public final class ModuleRegistry {

    private ModuleRegistry() {
    }

    /** 构建包含全部已知模块的工厂。各页面共用一个即可。 */
    public static ModuleFactory buildFactory() {
        return new ModuleFactory()
                // 首页
                .register(AssetOverviewModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new AssetOverviewModule(); }
                })
                .register(LatestSmsModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new LatestSmsModule(); }
                })
                .register(BannerModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new BannerModule(); }
                })
                .register(HomeLoopBannerModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new HomeLoopBannerModule(); }
                })
                .register(QuickEntryModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new QuickEntryModule(); }
                })
                .register(BadgeGridModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new BadgeGridModule(); }
                })
                .register(TransactionListModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new TransactionListModule(); }
                })
                // 财富
                .register(HoldingSummaryModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new HoldingSummaryModule(); }
                })
                .register(FundRecommendModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new FundRecommendModule(); }
                })
                .register(PromotionModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new PromotionModule(); }
                })
                // 贷款
                .register(CreditLimitModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new CreditLimitModule(); }
                })
                .register(LoanProductModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new LoanProductModule(); }
                })
                .register(RepayReminderModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new RepayReminderModule(); }
                })
                // 生活
                .register(LifeServiceModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new LifeServiceModule(); }
                })
                .register(BillCardModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new BillCardModule(); }
                })
                .register(CouponModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new CouponModule(); }
                })
                // 我的
                .register(ProfileHeaderModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new ProfileHeaderModule(); }
                })
                .register(AssetMiniModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new AssetMiniModule(); }
                })
                .register(SettingListModule.KEY, new ModuleFactory.Creator() {
                    public Module create() { return new SettingListModule(); }
                });
    }
}
