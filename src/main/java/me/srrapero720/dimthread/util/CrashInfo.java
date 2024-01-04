package me.srrapero720.dimthread.util;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.world.server.ServerWorld;

import static me.srrapero720.dimthread.DimThread.LOGGER;

public class CrashInfo {
    public final ServerWorld level;
    public final Throwable throwable;

    public CrashInfo(ServerWorld world, Throwable throwable) {
        this.level = world;
        this.throwable = throwable;
    }

    public void crash(String title) {
        CrashReport report = CrashReport.forThrowable(this.throwable, title);
        this.level.fillReportDetails(report);
        throw new ReportedException(report);
    }

    public void report(String title) {
        LOGGER.fatal(title, this.throwable);
    }
}