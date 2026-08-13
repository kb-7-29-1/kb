package com.salgosipo.gui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.List;

/**
 * Spacious Modern FlatLaf Swing Dialog with Normal Distribution Bell Curve Chart
 */
public class SafetySeedDialog extends JDialog {

    private boolean confirmed = false;

    public SafetySeedDialog(
            String destName,
            Integer destinationId,
            int fetchedCount,
            int existingRouteCount,
            int toCalculateCount,
            int savingPercent,
            int totalCctvCount,
            int totalLampCount,
            int totalPoliceCount,
            double avgScore,
            int maxScore,
            int minScore,
            List<Integer> existingScores) {
        try {
            FlatLightLaf.setup();
        } catch (Exception ignored) {
        }

        setTitle("살고싶오 - 안전 시드 연산 사전 검증 패널");
        setModal(true);
        setAlwaysOnTop(true);
        setSize(580, 720);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel rootPanel = new JPanel(new BorderLayout(0, 14));
        rootPanel.setBorder(new EmptyBorder(18, 20, 18, 20));
        rootPanel.setBackground(new Color(248, 250, 252));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout(0, 3));
        headerPanel.setOpaque(false);

        JLabel subTitle = new JLabel("SAFETY SEED BATCH ENGINE V2");
        subTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        subTitle.setForeground(new Color(37, 99, 235));

        JLabel mainTitle = new JLabel("안전 점수 시드 연산 사전 검증 보고서");
        mainTitle.setFont(new Font("Malgun Gothic", Font.BOLD, 19));
        mainTitle.setForeground(new Color(15, 23, 42));

        headerPanel.add(subTitle, BorderLayout.NORTH);
        headerPanel.add(mainTitle, BorderLayout.CENTER);
        rootPanel.add(headerPanel, BorderLayout.NORTH);

        // Body Card Container
        JPanel bodyContainer = new JPanel(new BorderLayout(0, 10));
        bodyContainer.setOpaque(false);

        // 1. Text Summary Card
        JPanel cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(new EmptyBorder(14, 16, 14, 16));
        cardPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 14");

        String htmlContent = String.format(
                "<html><body style='font-family: \"Malgun Gothic\", sans-serif; color: #1e293b; padding: 0; margin: 0;'>"
                        +

                        "<!-- Public Data Status -->" +
                        "<div style='font-size: 12px; font-weight: bold; color: #2563eb; margin-bottom: 6px; border-bottom: 1.5px solid #e2e8f0; padding-bottom: 4px;'>"
                        +
                        "📁 public_data/ 공공데이터 및 DB 기존점수 검증</div>" +

                        "<table style='width: 100%%; font-size: 11px; margin-bottom: 10px; border-collapse: collapse;'>"
                        +
                        "<tr style='height: 20px;'><td style='width: 160px; color: #64748b;'>🎥 CCTV 공공데이터:</td><td style='font-weight: bold; color: #059669;'>✅ 정상 로드 (%d건)</td><td style='color: #64748b;'>📊 DB 평균점수:</td><td style='font-weight: bold; color: #2563eb;'><b>%.1f점</b></td></tr>"
                        +
                        "<tr style='height: 20px;'><td style='color: #64748b;'>💡 보안등/가로등 데이터:</td><td style='font-weight: bold; color: #059669;'>✅ 정상 로드 (%d건)</td><td style='color: #64748b;'>🏆 DB 최고점수:</td><td style='font-weight: bold; color: #059669;'><b>%d점</b></td></tr>"
                        +
                        "<tr style='height: 20px;'><td style='color: #64748b;'>🚨 파출소/경찰서 데이터:</td><td style='font-weight: bold; color: #059669;'>✅ 정상 로드 (%d건)</td><td style='color: #64748b;'>⚠️ DB 최저점수:</td><td style='font-weight: bold; color: #e11d48;'><b>%d점</b></td></tr>"
                        +
                        "</table>" +

                        "<!-- Batch Target Info -->" +
                        "<div style='font-size: 12px; font-weight: bold; color: #2563eb; margin-bottom: 6px; border-bottom: 1.5px solid #e2e8f0; padding-bottom: 4px;'>"
                        +
                        "📋 시드 대상 및 API 낭비 차단 리포트</div>" +

                        "<table style='width: 100%%; font-size: 11px; border-collapse: collapse;'>" +
                        "<tr style='height: 20px;'><td style='width: 160px; color: #64748b;'>📍 목적지 정보:</td><td style='font-weight: bold; color: #0f172a;'><b>%s</b> (ID: %d)</td></tr>"
                        +
                        "<tr style='height: 20px;'><td style='color: #64748b;'>📊 검색된 전체 매물:</td><td style='font-weight: bold; color: #0f172a;'><b>%d건</b></td></tr>"
                        +
                        "<tr style='height: 20px;'><td style='color: #059669;'>♻️ DB LineString 100%% 재탕:</td><td style='font-weight: bold; color: #059669;'><b>%d건</b> (TMAP API 0회 호출!)</td></tr>"
                        +
                        "<tr style='height: 20px;'><td style='color: #d97706;'>⚡ TMAP API 신규 호출:</td><td style='font-weight: bold; color: #d97706;'><b>%d건</b></td></tr>"
                        +
                        "<tr style='height: 20px;'><td style='color: #2563eb;'>💰 API 낭비 차단율:</td><td style='font-weight: bold; color: #2563eb;'><b>%d%% 절감 (API 낭비 0건)</b></td></tr>"
                        +
                        "</table>" +

                        "</body></html>",
                totalCctvCount, avgScore,
                totalLampCount, maxScore,
                totalPoliceCount, minScore,
                destName, destinationId, fetchedCount, existingRouteCount, toCalculateCount, savingPercent);

        JLabel bodyLabel = new JLabel(htmlContent);
        cardPanel.add(bodyLabel, BorderLayout.CENTER);
        bodyContainer.add(cardPanel, BorderLayout.NORTH);

        // 2. Normal Distribution Chart Panel
        NormalDistributionPanel chartPanel = new NormalDistributionPanel(existingScores, avgScore, minScore, maxScore);
        bodyContainer.add(chartPanel, BorderLayout.CENTER);

        // Prompt Bar
        JLabel promptLabel = new JLabel("위 사전 검증 결과 및 점수 정규분포에 따라 시드 배치를 실행할까요?");
        promptLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        promptLabel.setForeground(new Color(30, 58, 138));
        promptLabel.setHorizontalAlignment(SwingConstants.CENTER);
        promptLabel.setOpaque(true);
        promptLabel.setBackground(new Color(239, 246, 255));
        promptLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(191, 219, 254), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        bodyContainer.add(promptLabel, BorderLayout.SOUTH);

        rootPanel.add(bodyContainer, BorderLayout.CENTER);

        // Footer Action Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        buttonPanel.setOpaque(false);

        JButton confirmBtn = new JButton("예 (Y) - 배치 실행");
        confirmBtn.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        confirmBtn.setBackground(new Color(37, 99, 235));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 10; buttonType: default");
        confirmBtn.setPreferredSize(new Dimension(0, 44));

        JButton cancelBtn = new JButton("아니오 (N) - 취소");
        cancelBtn.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        cancelBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        cancelBtn.setPreferredSize(new Dimension(0, 44));

        confirmBtn.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        cancelBtn.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        buttonPanel.add(confirmBtn);
        buttonPanel.add(cancelBtn);
        rootPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public static boolean showDialog(
            String destName,
            Integer destinationId,
            int fetchedCount,
            int existingRouteCount,
            int toCalculateCount,
            int savingPercent,
            int totalCctvCount,
            int totalLampCount,
            int totalPoliceCount,
            double avgScore,
            int maxScore,
            int minScore,
            List<Integer> existingScores) {
        try {
            System.setProperty("java.awt.headless", "false");
            FlatLightLaf.setup();
            SafetySeedDialog dialog = new SafetySeedDialog(
                    destName, destinationId, fetchedCount, existingRouteCount,
                    toCalculateCount, savingPercent, totalCctvCount, totalLampCount, totalPoliceCount,
                    avgScore, maxScore, minScore, existingScores);
            dialog.setAlwaysOnTop(true);
            dialog.toFront();
            dialog.setVisible(true);
            return dialog.isConfirmed();
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    /**
     * Custom Swing Component: Normal Distribution Bell Curve Chart Panel
     */
    static class NormalDistributionPanel extends JPanel {
        private final List<Integer> scores;
        private final double avg;
        private final int min;
        private final int max;

        public NormalDistributionPanel(List<Integer> scores, double avg, int min, int max) {
            this.scores = scores;
            this.avg = avg;
            this.min = min;
            this.max = max;
            setOpaque(false);
            setPreferredSize(new Dimension(0, 160));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background Card Container
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w, h, 14, 14);
            g2.setColor(new Color(226, 232, 240));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

            // Title
            g2.setColor(new Color(30, 41, 59));
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 12));
            g2.drawString("📊 기존 DB 안전점수 정규분포 곡선 (Bell Curve)", 14, 20);

            if (scores == null || scores.isEmpty()) {
                g2.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
                g2.setColor(new Color(148, 163, 184));
                g2.drawString("기존 DB 안전 점수 데이터가 없어 기본 분포 곡선을 표기합니다.", 14, h / 2 + 5);
                g2.dispose();
                return;
            }

            int padLeft = 35;
            int padRight = 35;
            int padTop = 32;
            int padBottom = 28;

            int chartX = padLeft;
            int chartW = w - padLeft - padRight;
            int chartY = padTop;
            int chartH = h - padTop - padBottom;

            // X-Axis range starting from MIN score to MAX score
            double startScore = Math.max(0, min - 2);
            double endScore = Math.min(100, max + 2);
            double range = Math.max(1.0, endScore - startScore);

            // 1. Compute Histogram Buckets across range
            int[] buckets = new int[10];
            for (int s : scores) {
                int idx = Math.min(9, Math.max(0, (int) (((s - startScore) / range) * 10)));
                buckets[idx]++;
            }
            int maxFreq = 1;
            for (int b : buckets) if (b > maxFreq) maxFreq = b;

            // Draw Translucent Histogram Bars
            int barWidth = chartW / 10;
            for (int i = 0; i < 10; i++) {
                int bh = (int) ((double) buckets[i] / maxFreq * (chartH - 12));
                int bx = chartX + i * barWidth;
                int by = chartY + chartH - bh;

                g2.setColor(new Color(219, 234, 254, 160));
                g2.fillRect(bx + 1, by, barWidth - 2, bh);
                g2.setColor(new Color(147, 197, 253));
                g2.drawRect(bx + 1, by, barWidth - 2, bh);
            }

            // 2. Compute Mean & Standard Deviation
            double mean = avg;
            double variance = 0;
            for (int s : scores) variance += Math.pow(s - mean, 2);
            double stdDev = Math.sqrt(variance / scores.size());
            if (stdDev < 1.0) stdDev = 12.0;

            // 3. Draw Gaussian Bell Curve Path from startScore to endScore
            GeneralPath path = new GeneralPath();
            for (int xPx = 0; xPx <= chartW; xPx++) {
                double scoreVal = startScore + ((double) xPx / chartW) * range;
                double gauss = Math.exp(-0.5 * Math.pow((scoreVal - mean) / stdDev, 2));
                int yPx = chartY + chartH - (int) (gauss * (chartH - 10));
                if (xPx == 0) {
                    path.moveTo(chartX + xPx, yPx);
                } else {
                    path.lineTo(chartX + xPx, yPx);
                }
            }

            // Fill Blue Gradient under Bell Curve
            GeneralPath fillPath = (GeneralPath) path.clone();
            fillPath.lineTo(chartX + chartW, chartY + chartH);
            fillPath.lineTo(chartX, chartY + chartH);
            fillPath.closePath();

            GradientPaint gp = new GradientPaint(0, chartY, new Color(37, 99, 235, 110), 0, chartY + chartH, new Color(147, 197, 253, 10));
            g2.setPaint(gp);
            g2.fill(fillPath);

            // Draw Main Bell Curve Line
            g2.setColor(new Color(37, 99, 235));
            g2.setStroke(new BasicStroke(2.2f));
            g2.draw(path);

            // 4. Mean Line Indicator (Crimson Red)
            int meanX = chartX + (int) (((mean - startScore) / range) * chartW);
            g2.setColor(new Color(225, 29, 72));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 3.0f}, 0.0f));
            g2.drawLine(meanX, chartY - 4, meanX, chartY + chartH);

            // 5. Min Line Indicator (Amber Gold)
            int minX = chartX + (int) (((min - startScore) / range) * chartW);
            g2.setColor(new Color(217, 119, 6));
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{2.0f, 2.0f}, 0.0f));
            g2.drawLine(minX, chartY + 10, minX, chartY + chartH);

            // 6. Max Line Indicator (Emerald Green)
            int maxX = chartX + (int) (((max - startScore) / range) * chartW);
            g2.setColor(new Color(5, 150, 105));
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{2.0f, 2.0f}, 0.0f));
            g2.drawLine(maxX, chartY + 10, maxX, chartY + chartH);

            // Labels Below Chart
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 10));
            g2.setColor(new Color(225, 29, 72));
            g2.drawString(String.format("평균 μ:%.1f점", mean), Math.min(w - 75, Math.max(10, meanX - 25)), chartY - 6);

            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 10));
            g2.setColor(new Color(100, 116, 139));
            g2.drawString(String.format("%d점(최저)", min), chartX - 10, chartY + chartH + 16);
            g2.drawString(String.format("%d점(최고)", max), chartX + chartW - 35, chartY + chartH + 16);

            g2.dispose();
        }
    }
}
