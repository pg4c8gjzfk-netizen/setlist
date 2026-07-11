package jp.ac.u_tokai.cc.javaadvanced;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

/** 自動生成、編集画面への遷移、既存形式での出力を行うメイン画面です。 */
public class SetlistFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String DATA_DIRECTORY = "Data";

    private final JComboBox<File> dataFileBox;
    private final JTextArea resultArea;
    private final JLabel sheetHintLabel;
    private final JButton editButton;
    private final JButton generateButton;
    private final JButton startEditingButton;
    private final JButton newButton;
    private final JButton openProjectButton;
    private final JButton excelButton;
    private SetlistProject currentProject;
    private boolean projectAvailable;

    /** メイン画面を作成します。 */
    public SetlistFrame() {
        super("Setlist Studio");
        this.dataFileBox = new JComboBox<>();
        this.resultArea = new JTextArea(22, 70);
        this.sheetHintLabel = AppTheme.body("XLSXファイルを選択してください。");
        this.editButton = AppTheme.secondaryButton("編集");
        this.generateButton = AppTheme.secondaryButton("生成");
        this.startEditingButton = AppTheme.primaryButton("編集を開始");
        this.newButton = AppTheme.quietButton("新規作成");
        this.openProjectButton = AppTheme.quietButton("編集済みXLSXを開く");
        this.excelButton = AppTheme.quietButton("配布用XLSX出力");
        this.currentProject = SetlistProjectFactory.newEmptyProject();
        this.projectAvailable = false;

        setupWindow();
        dataFileBox.addActionListener(event -> updateSelectedFileHint());
        loadDataFiles();
    }

    private void setupWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(AppIcon.create(64));
        JPanel root = AppTheme.page(new BorderLayout(0, 18));
        root.setBorder(AppTheme.pagePadding());
        root.setFocusable(true);
        setContentPane(root);

        DefaultListCellRenderer fileRenderer = new DefaultListCellRenderer();
        dataFileBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            Component rendered = fileRenderer.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            if (rendered instanceof JLabel label) {
                label.setText(value == null ? "" : value.getName());
            }
            return rendered;
        });

        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setText("まだプレビューはありません。\nデータを選び、「編集を開始」または「生成」を選択してください。");
        AppTheme.stylePreview(resultArea);
        editButton.setEnabled(false);
        excelButton.setEnabled(false);

        root.add(createHeaderPanel(), BorderLayout.NORTH);

        JPanel workspace = AppTheme.page(new BorderLayout(0, 16));
        workspace.add(createInputPanel(), BorderLayout.NORTH);
        workspace.add(createPreviewPanel(), BorderLayout.CENTER);
        root.add(workspace, BorderLayout.CENTER);
        root.add(createActionPanel(), BorderLayout.SOUTH);

        getRootPane().setDefaultButton(startEditingButton);
        AppTheme.bindMenuShortcut(getRootPane(), "new-project", KeyEvent.VK_N, newButton::doClick);
        AppTheme.bindMenuShortcut(getRootPane(), "open-project", KeyEvent.VK_O, openProjectButton::doClick);
        AppTheme.bindMenuShortcut(getRootPane(), "generate-setlist", KeyEvent.VK_G, generateButton::doClick);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                root.requestFocusInWindow();
            }
        });
        setMinimumSize(new Dimension(980, 660));
        setSize(1120, 760);
        setLocationRelativeTo(null);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = AppTheme.page(new BorderLayout(20, 0));
        JPanel titleStack = transparentBoxPanel(BoxLayout.Y_AXIS);
        titleStack.add(AppTheme.title("Setlist Studio"));
        titleStack.add(Box.createVerticalStrut(5));
        titleStack.add(AppTheme.body("出演者の流れを整え、迷いなく本番へ進める香盤表ワークスペース"));

        JLabel localBadge = AppTheme.statusPill("ローカル編集", new java.awt.Color(0x248A3D));

        panel.add(titleStack, BorderLayout.WEST);
        panel.add(compactHolder(localBadge), BorderLayout.EAST);
        return panel;
    }

    private JPanel createInputPanel() {
        JPanel panel = AppTheme.card(new BorderLayout(0, 16));

        JPanel heading = transparentBoxPanel(BoxLayout.Y_AXIS);
        heading.add(AppTheme.heading("入力ファイル"));
        heading.add(Box.createVerticalStrut(4));
        heading.add(AppTheme.body("各ワークシートを独立した公演として保持し、シート内の曲順だけを生成します。"));
        panel.add(heading, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 12);

        constraints.gridx = 0;
        constraints.weightx = 1.0;
        fields.add(createFieldGroup("XLSXファイル", dataFileBox, 500), constraints);

        JButton reloadButton = AppTheme.quietButton("再読込");
        reloadButton.setToolTipText("Dataフォルダの入力ファイル一覧を更新します");
        reloadButton.addActionListener(event -> loadDataFiles());
        constraints.gridx = 1;
        constraints.weightx = 0;
        constraints.anchor = GridBagConstraints.SOUTHWEST;
        fields.add(reloadButton, constraints);

        panel.add(fields, BorderLayout.CENTER);

        sheetHintLabel.setForeground(AppTheme.ACCENT);
        panel.add(sheetHintLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createPreviewPanel() {
        JPanel panel = AppTheme.card(new BorderLayout(0, 14));
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel text = transparentBoxPanel(BoxLayout.Y_AXIS);
        text.add(AppTheme.heading("香盤表プレビュー"));
        text.add(Box.createVerticalStrut(3));
        text.add(AppTheme.body("生成結果と現在の編集状態を、保存前に確認できます。"));
        header.add(text, BorderLayout.WEST);

        editButton.setToolTipText("現在の香盤表を公演タブで編集します");
        header.add(editButton, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JScrollPane previewScroll = AppTheme.scroll(resultArea);
        previewScroll.setBorder(BorderFactory.createLineBorder(new java.awt.Color(0xE7E7EB)));
        panel.add(previewScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createActionPanel() {
        JPanel panel = AppTheme.page(new BorderLayout(16, 0));
        generateButton.addActionListener(event -> generateSetlist());
        startEditingButton.addActionListener(event -> startEditingSelectedData());
        newButton.addActionListener(event -> openEditor(SetlistProjectFactory.newEmptyProject()));
        openProjectButton.addActionListener(event -> openSavedProject());
        editButton.addActionListener(event -> openEditor(currentProject));
        excelButton.addActionListener(event -> exportCurrentProject());

        newButton.setToolTipText("空の香盤表から作成します（Ctrl/Command+N）");
        openProjectButton.setToolTipText("編集状態を保持したXLSXを開きます（Ctrl/Command+O）");
        generateButton.setToolTipText("入力データから曲順を自動生成します（Ctrl/Command+G）");
        startEditingButton.setToolTipText("入力データをそのまま公演タブで開きます");
        excelButton.setToolTipText("現在のプレビュー内容を配布用XLSXとして保存します");

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftActions.setOpaque(false);
        leftActions.add(newButton);
        leftActions.add(openProjectButton);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);
        rightActions.add(excelButton);
        rightActions.add(generateButton);
        rightActions.add(startEditingButton);

        panel.add(leftActions, BorderLayout.WEST);
        panel.add(rightActions, BorderLayout.EAST);
        return panel;
    }

    private JPanel createFieldGroup(String labelText, Component component, int width) {
        JPanel group = transparentBoxPanel(BoxLayout.Y_AXIS);
        JLabel label = AppTheme.fieldLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setPreferredSize(new Dimension(width, 38));
        component.setMinimumSize(new Dimension(Math.min(width, 80), 38));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        if (component instanceof JComponent swingComponent) {
            swingComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        group.add(label);
        group.add(Box.createVerticalStrut(6));
        group.add(component);
        return group;
    }

    private JPanel compactHolder(Component component) {
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(component);
        return holder;
    }

    private JPanel transparentBoxPanel(int axis) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, axis));
        return panel;
    }

    private void loadDataFiles() {
        dataFileBox.removeAllItems();
        File dataDir = new File(DATA_DIRECTORY);
        File[] files = dataDir.listFiles((directory, name) ->
                name.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx"));
        if (files == null || files.length == 0) {
            sheetHintLabel.setText("DataフォルダにXLSXファイルがありません。");
            resultArea.setText("入力データが見つかりません。\nDataフォルダへXLSXを追加し、「再読込」を押してください。");
            return;
        }
        for (File file : files) {
            dataFileBox.addItem(file);
        }
        updateSelectedFileHint();
    }

    private void generateSetlist() {
        File selectedFile = (File) dataFileBox.getSelectedItem();
        if (selectedFile == null) {
            showError("読み込むデータファイルを選択してください。");
            return;
        }

        List<PerformanceSheet> sourceSheets;
        try {
            sourceSheets = loadPerformanceSheets(selectedFile);
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
            return;
        }
        if (sourceSheets.isEmpty()) {
            showError("演目データを読み込めませんでした。");
            return;
        }

        List<PerformanceSheet> generatedSheets = new SetlistGenerator().generateWithinSheets(sourceSheets);
        displayGeneratedProject(SetlistProjectFactory.fromImportedSheets(generatedSheets));
    }

    private List<PerformanceSheet> loadPerformanceSheets(File selectedFile) {
        return new XlsxPerformanceReader().loadSheets(selectedFile);
    }

    private void startEditingSelectedData() {
        File selectedFile = (File) dataFileBox.getSelectedItem();
        if (selectedFile == null) {
            showError("読み込むデータファイルを選択してください。");
            return;
        }
        List<PerformanceSheet> performanceSheets;
        try {
            performanceSheets = loadPerformanceSheets(selectedFile);
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
            return;
        }
        if (performanceSheets.isEmpty()) {
            showError("演目データを読み込めませんでした。");
            return;
        }
        openEditor(SetlistProjectFactory.fromImportedSheets(performanceSheets));
    }

    /** 選択中のXLSXとシート境界の扱いを案内します。 */
    private void updateSelectedFileHint() {
        File selectedFile = (File) dataFileBox.getSelectedItem();
        dataFileBox.setToolTipText(selectedFile == null ? null : selectedFile.getAbsolutePath());

        if (selectedFile == null) {
            return;
        }
        sheetHintLabel.setText("● 各シートを独立した公演として保持し、シート間の再配分は行いません。");
    }

    /**
     * 自動生成済みのプロジェクトを画面へ反映します。
     *
     * <p>編集ボタンは、生成結果を表示した後だけ有効になります。</p>
     *
     * @param generatedProject 自動生成したプロジェクト
     */
    void displayGeneratedProject(SetlistProject generatedProject) {
        updateCurrentProject(generatedProject);
    }

    private void openEditor(SetlistProject project) {
        updateCurrentProject(project);
        SetlistEditorFrame editor = new SetlistEditorFrame(project, this::updateCurrentProject);
        editor.setVisible(true);
    }

    private void updateCurrentProject(SetlistProject project) {
        currentProject = Objects.requireNonNull(project, "project must not be null");
        projectAvailable = !currentProject.sessions().isEmpty();
        editButton.setEnabled(projectAvailable);
        excelButton.setEnabled(projectAvailable);
        resultArea.setText(formatProject(currentProject));
    }

    private void openSavedProject() {
        JFileChooser chooser = new JFileChooser(new File("Data/output"));
        chooser.setDialogTitle("編集可能な香盤表XLSXを開く");
        chooser.setFileFilter(new FileNameExtensionFilter("Excelファイル (*.xlsx)", "xlsx"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            SetlistProject loadedProject = new XlsxSetlistProjectReader().read(chooser.getSelectedFile());
            openEditor(loadedProject);
        } catch (IOException | IllegalArgumentException exception) {
            showError("編集可能な香盤表XLSXを開けませんでした: " + exception.getMessage());
        }
    }

    private void exportCurrentProject() {
        if (!projectAvailable) {
            showError("先に香盤表を作成または読み込んでください。");
            return;
        }

        JFileChooser chooser = new JFileChooser(new File("Data/output"));
        chooser.setDialogTitle("現在の香盤表を配布用XLSXとして保存");
        chooser.setFileFilter(new FileNameExtensionFilter("Excelファイル (*.xlsx)", "xlsx"));
        chooser.setSelectedFile(new File("Data/output", "output_setlist.xlsx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File outputFile = ensureXlsxExtension(chooser.getSelectedFile());
        if (outputFile.exists() && JOptionPane.showConfirmDialog(
                this,
                outputFile.getName() + " を上書きしますか？",
                "上書き確認",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            File savedFile = writeCurrentProject(outputFile);
            JOptionPane.showMessageDialog(this, "配布用XLSXを保存しました: " + savedFile.getAbsolutePath());
        } catch (IOException | IllegalArgumentException exception) {
            showError("XLSXの出力に失敗しました: " + exception.getMessage());
        }
    }

    /** 現在の編集内容を指定先へ書き出します。回帰テストからも使用します。 */
    File writeCurrentProject(File outputFile) throws IOException {
        if (!projectAvailable) {
            throw new IllegalStateException("出力できる香盤表がありません。");
        }
        return new XlsxSetlistExporter().export(currentProject, outputFile);
    }

    private File ensureXlsxExtension(File file) {
        return file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")
                ? file
                : new File(file.getParentFile(), file.getName() + ".xlsx");
    }

    private String formatProject(SetlistProject project) {
        StringBuilder builder = new StringBuilder();
        for (SetlistSession session : project.sessions()) {
            builder.append("【").append(session.name()).append("】\n");
            for (int index = 0; index < session.entries().size(); index++) {
                SetlistEntry entry = session.entries().get(index);
                builder.append(index + 1)
                        .append(". ")
                        .append(entry.title())
                        .append(" / ")
                        .append(String.join(", ", entry.performers()));
                if (entry.fixed()) {
                    builder.append(" [固定: ").append(entry.fixedPosition()).append("]");
                }
                builder.append('\n');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "エラー", JOptionPane.ERROR_MESSAGE);
    }

    /** メイン画面を表示します。 */
    public void showScreen() {
        setVisible(true);
    }
}
