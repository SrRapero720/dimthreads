package me.srrapero720.dimthread.util;

import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.server.level.ServerLevel;

import static me.srrapero720.dimthread.DimThread.LOGGER;

public record CrashInfo(ServerLevel world, Throwable throwable) {

    public void crash(String title) {
        CrashReport report = CrashReport.forThrowable(this.throwable, title);
        this.world.fillReportDetails(report);
        throw new ReportedException(report);
    }

    public void report(String title) {
        LOGGER.fatal(title, this.throwable);
    }
}