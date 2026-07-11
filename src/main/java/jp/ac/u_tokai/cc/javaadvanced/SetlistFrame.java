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
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

/** 自動生成、編集画面への遷移、既存形式での出力を行うメイン画面です。 */
public class SetlistFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final JTextField inputFileField;
    private final JTextArea resultArea;
    private final JLabel sheetHintLabel;
    private final JButton editButton;
    private final JButton generateButton;
    private final JButton startEditingButton;
    private final JButton newButton;
    private final JButton openProjectButton;
    private final JButton saveProjectButton;
    private final JButton excelButton;
    private final JLabel projectStatusLabel;
    private final UnsavedChangesPrompt unsavedChangesPrompt;
    private final AppFileLocations fileLocations;
    private File selectedInputFile;
    private SetlistProject currentProject;
    private boolean projectAvailable;
    private boolean unsavedChanges;
    private File editableProjectFile;
    private SetlistEditorFrame activeEditor;

    /** メイン画面を作成します。 */
    public SetlistFrame() {
        this(UnsavedChangesPrompt.swingDialog(), new AppFileLocations());
    }

    SetlistFrame(UnsavedChangesPrompt unsavedChangesPrompt) {
        this(unsavedChangesPrompt, new AppFileLocations());
    }

    SetlistFrame(UnsavedChangesPrompt unsavedChangesPrompt, AppFileLocations fileLocations) {
        super("Setlist Studio");
        this.unsavedChangesPrompt = Objects.requireNonNull(
                unsavedChangesPrompt, "unsavedChangesPrompt must not be null");
        this.fileLocations = Objects.requireNonNull(fileLocations, "fileLocations must not be null");
        this.inputFileField = new JTextField("XLSXファイルが選択されていません");
        this.resultArea = new JTextArea(22, 70);
        this.sheetHintLabel = AppTheme.body("任意の場所にあるXLSXファイルを選択してください。");
        this.editButton = AppTheme.secondaryButton("編集");
        this.generateButton = AppTheme.secondaryButton("生成");
        this.startEditingButton = AppTheme.primaryButton("編集を開始");
        this.newButton = AppTheme.quietButton("新規作成");
        this.openProjectButton = AppTheme.quietButton("編集済みXLSXを開く");
        this.saveProjectButton = AppTheme.quietButton("編集状態を保存");
        this.excelButton = AppTheme.quietButton("配布用XLSX出力");
        this.projectStatusLabel = AppTheme.statusPill("未作成", AppTheme.TEXT_SECONDARY);
        this.currentProject = SetlistProjectFactory.newEmptyProject();
        this.projectAvailable = false;
        this.unsavedChanges = false;

        setupWindow();
    }

    private void setupWindow() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setIconImage(AppIcon.create(64));
        JPanel root = AppTheme.page(new BorderLayout(0, 18));
        root.setBorder(AppTheme.pagePadding());
        root.setFocusable(true);
        setContentPane(root);

        inputFileField.setEditable(false);
        inputFileField.setToolTipText("「XLSXを選択」から入力ファイルを指定してください。");
        inputFileField.getAccessibleContext().setAccessibleName("選択中の入力XLSXファイル");

        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setText("まだプレビューはありません。\nデータを選び、「編集を開始」または「生成」を選択してください。");
        AppTheme.stylePreview(resultArea);
        editButton.setEnabled(false);
        generateButton.setEnabled(false);
        startEditingButton.setEnabled(false);
        saveProjectButton.setEnabled(false);
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
        AppTheme.bindMenuShortcut(getRootPane(), "save-project", KeyEvent.VK_S, saveProjectButton::doClick);
        AppTheme.bindMenuShortcut(getRootPane(), "generate-setlist", KeyEvent.VK_G, generateButton::doClick);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                root.requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent event) {
                requestApplicationClose();
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

        panel.add(titleStack, BorderLayout.WEST);
        panel.add(compactHolder(projectStatusLabel), BorderLayout.EAST);
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
        fields.add(createFieldGroup("入力XLSX", inputFileField, 500), constraints);

        JButton chooseInputButton = AppTheme.quietButton("XLSXを選択");
        chooseInputButton.setToolTipText("任意の場所にある入力XLSXファイルを選択します");
        chooseInputButton.addActionListener(event -> chooseInputFile());
        constraints.gridx = 1;
        constraints.weightx = 0;
        constraints.anchor = GridBagConstraints.SOUTHWEST;
        fields.add(chooseInputButton, constraints);

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
        newButton.addActionListener(event -> createNewProject());
        openProjectButton.addActionListener(event -> openSavedProject());
        saveProjectButton.addActionListener(event -> saveEditableProject(true));
        editButton.addActionListener(event -> openCurrentProjectEditor());
        excelButton.addActionListener(event -> exportCurrentProject());

        newButton.setToolTipText("空の香盤表から作成します（Ctrl/Command+N）");
        openProjectButton.setToolTipText("編集状態を保持したXLSXを開きます（Ctrl/Command+O）");
        saveProjectButton.setToolTipText("再編集できる状態で保存します（Ctrl/Command+S）");
        generateButton.setToolTipText("入力データから曲順を自動生成します（Ctrl/Command+G）");
        startEditingButton.setToolTipText("入力データをそのまま公演タブで開きます");
        excelButton.setToolTipText("現在のプレビュー内容を配布用XLSXとして保存します");

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftActions.setOpaque(false);
        leftActions.add(newButton);
        leftActions.add(openProjectButton);
        leftActions.add(saveProjectButton);

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

    private void chooseInputFile() {
        JFileChooser chooser = new JFileChooser(fileLocations.inputDirectory());
        chooser.setDialogTitle("入力XLSXを選択");
        chooser.setFileFilter(new FileNameExtensionFilter("Excelファイル (*.xlsx)", "xlsx"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            setSelectedInputFile(chooser.getSelectedFile());
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    /** 入力XLSXを選択し、生成・編集操作を有効化します。 */
    void setSelectedInputFile(File inputFile) {
        if (inputFile == null
                || !inputFile.isFile()
                || !inputFile.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("読み込むXLSXファイルを選択してください。");
        }
        selectedInputFile = inputFile.getAbsoluteFile();
        fileLocations.rememberInputFile(selectedInputFile);
        inputFileField.setText(selectedInputFile.getName());
        inputFileField.setToolTipText(selectedInputFile.getAbsolutePath());
        generateButton.setEnabled(true);
        startEditingButton.setEnabled(true);
        sheetHintLabel.setText("● 選択済み。各シートを独立した公演として保持し、シート間の再配分は行いません。");
    }

    private void generateSetlist() {
        File selectedFile = selectedInputFile;
        if (selectedFile == null) {
            showError("読み込むXLSXファイルを選択してください。");
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
        SetlistProject generatedProject = SetlistProjectFactory.fromImportedSheets(generatedSheets);
        if (!confirmProjectReplacement("入力データから生成する")) {
            return;
        }
        closeActiveEditor();
        replaceCurrentProject(generatedProject, true, null);
    }

    private List<PerformanceSheet> loadPerformanceSheets(File selectedFile) {
        return new XlsxPerformanceReader().loadSheets(selectedFile);
    }

    private void startEditingSelectedData() {
        File selectedFile = selectedInputFile;
        if (selectedFile == null) {
            showError("読み込むXLSXファイルを選択してください。");
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
        if (!confirmProjectReplacement("入力データの編集を開始する")) {
            return;
        }
        closeActiveEditor();
        replaceCurrentProject(SetlistProjectFactory.fromImportedSheets(performanceSheets), false, null);
        openCurrentProjectEditor();
    }

    /**
     * 自動生成済みのプロジェクトを画面へ反映します。
     *
     * <p>編集ボタンは、生成結果を表示した後だけ有効になります。</p>
     *
     * @param generatedProject 自動生成したプロジェクト
     */
    void displayGeneratedProject(SetlistProject generatedProject) {
        replaceCurrentProject(generatedProject, true, null);
    }

    private void createNewProject() {
        if (!confirmProjectReplacement("新しい香盤表を作成する")) {
            return;
        }
        closeActiveEditor();
        replaceCurrentProject(SetlistProjectFactory.newEmptyProject(), false, null);
        openCurrentProjectEditor();
    }

    private void openCurrentProjectEditor() {
        if (activeEditor != null && activeEditor.isDisplayable()) {
            activeEditor.toFront();
            activeEditor.requestFocus();
            return;
        }
        SetlistEditorFrame editor = new SetlistEditorFrame(
                currentProject,
                this::handleProjectChanged,
                this::handleProjectSaved,
                unsavedChanges,
                fileLocations);
        activeEditor = editor;
        editor.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                if (activeEditor == editor) {
                    activeEditor = null;
                }
            }
        });
        editor.setVisible(true);
    }

    private void handleProjectChanged(SetlistProject project) {
        replaceCurrentProject(project, true, editableProjectFile);
    }

    private void handleProjectSaved(SetlistProject project, File outputFile) {
        fileLocations.rememberOutputFile(outputFile);
        replaceCurrentProject(project, false, outputFile);
    }

    private void replaceCurrentProject(SetlistProject project, boolean dirty, File savedFile) {
        currentProject = Objects.requireNonNull(project, "project must not be null");
        projectAvailable = !currentProject.sessions().isEmpty();
        unsavedChanges = dirty;
        editableProjectFile = savedFile == null ? null : savedFile.getAbsoluteFile();
        editButton.setEnabled(projectAvailable);
        saveProjectButton.setEnabled(projectAvailable);
        excelButton.setEnabled(projectAvailable);
        resultArea.setText(formatProject(currentProject));
        updateProjectStatus();
    }

    private void openSavedProject() {
        JFileChooser chooser = new JFileChooser(fileLocations.outputDirectory());
        chooser.setDialogTitle("編集可能な香盤表XLSXを開く");
        chooser.setFileFilter(new FileNameExtensionFilter("Excelファイル (*.xlsx)", "xlsx"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            File inputFile = chooser.getSelectedFile().getAbsoluteFile();
            SetlistProject loadedProject = new XlsxSetlistProjectReader().read(inputFile);
            if (!confirmProjectReplacement("編集済みXLSXを開く")) {
                return;
            }
            fileLocations.rememberOutputFile(inputFile);
            closeActiveEditor();
            replaceCurrentProject(loadedProject, false, inputFile);
            openCurrentProjectEditor();
        } catch (IOException | IllegalArgumentException exception) {
            showError("編集可能な香盤表XLSXを開けませんでした: " + exception.getMessage());
        }
    }

    private boolean saveEditableProject(boolean showCompletionMessage) {
        if (!projectAvailable) {
            showError("先に香盤表を作成または読み込んでください。");
            return false;
        }
        synchronizeActiveEditor();
        File outputFile = editableProjectFile;
        if (outputFile == null) {
            JFileChooser chooser = new JFileChooser(fileLocations.outputDirectory());
            chooser.setDialogTitle("編集状態を保存");
            chooser.setFileFilter(new FileNameExtensionFilter("Excelファイル (*.xlsx)", "xlsx"));
            chooser.setSelectedFile(fileLocations.defaultOutputFile("setlist-project.xlsx"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return false;
            }
            outputFile = ensureXlsxExtension(chooser.getSelectedFile()).getAbsoluteFile();
            if (outputFile.exists() && JOptionPane.showConfirmDialog(
                    this,
                    outputFile.getName() + " を上書きしますか？",
                    "上書き確認",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        try {
            File savedFile = writeEditableProject(outputFile);
            if (showCompletionMessage) {
                JOptionPane.showMessageDialog(this, "編集状態を保存しました: " + savedFile.getAbsolutePath());
            }
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            showError("編集状態の保存に失敗しました: " + exception.getMessage());
            return false;
        }
    }

    /** 現在の編集状態を再読込可能なXLSXとして保存します。 */
    File writeEditableProject(File outputFile) throws IOException {
        if (!projectAvailable) {
            throw new IllegalStateException("保存できる香盤表がありません。");
        }
        File absoluteFile = outputFile.getAbsoluteFile();
        if (activeEditor != null && activeEditor.isDisplayable()) {
            return activeEditor.writeEditableProject(absoluteFile);
        }
        new XlsxSetlistProjectWriter().write(currentProject, absoluteFile);
        fileLocations.rememberOutputFile(absoluteFile);
        replaceCurrentProject(currentProject, false, absoluteFile);
        return absoluteFile;
    }

    /** 未保存内容がある場合だけ、保存・破棄・キャンセルを確認します。 */
    boolean confirmProjectReplacement(String actionName) {
        synchronizeActiveEditor();
        if (!unsavedChanges) {
            return true;
        }
        return switch (unsavedChangesPrompt.ask(this, actionName)) {
            case SAVE -> saveEditableProject(false);
            case DISCARD -> true;
            case CANCEL -> false;
        };
    }

    boolean hasUnsavedChanges() {
        return unsavedChanges;
    }

    private void synchronizeActiveEditor() {
        if (activeEditor != null && activeEditor.isDisplayable()) {
            activeEditor.commitPendingEdits();
        }
    }

    private void closeActiveEditor() {
        if (activeEditor != null) {
            SetlistEditorFrame editor = activeEditor;
            activeEditor = null;
            editor.dispose();
        }
    }

    private void requestApplicationClose() {
        if (!confirmProjectReplacement("アプリを終了する")) {
            return;
        }
        closeActiveEditor();
        dispose();
    }

    private void updateProjectStatus() {
        if (!projectAvailable) {
            AppTheme.updateStatusPill(projectStatusLabel, "未作成", AppTheme.TEXT_SECONDARY);
            projectStatusLabel.setToolTipText("香盤表はまだ作成されていません。");
            setTitle("Setlist Studio");
            return;
        }
        if (unsavedChanges) {
            AppTheme.updateStatusPill(
                    projectStatusLabel, "未保存の変更", new java.awt.Color(0xB54708));
            projectStatusLabel.setToolTipText("編集状態を保存すると、次回も再編集できます。");
            setTitle("Setlist Studio *");
            return;
        }
        if (editableProjectFile != null) {
            AppTheme.updateStatusPill(projectStatusLabel, "保存済み", new java.awt.Color(0x248A3D));
            projectStatusLabel.setToolTipText(editableProjectFile.getAbsolutePath());
            setTitle("Setlist Studio");
            return;
        }
        AppTheme.updateStatusPill(projectStatusLabel, "変更なし", AppTheme.TEXT_SECONDARY);
        projectStatusLabel.setToolTipText("元データから変更されていません。");
        setTitle("Setlist Studio");
    }

    private void exportCurrentProject() {
        if (!projectAvailable) {
            showError("先に香盤表を作成または読み込んでください。");
            return;
        }

        JFileChooser chooser = new JFileChooser(fileLocations.outputDirectory());
        chooser.setDialogTitle("現在の香盤表を配布用XLSXとして保存");
        chooser.setFileFilter(new FileNameExtensionFilter("Excelファイル (*.xlsx)", "xlsx"));
        chooser.setSelectedFile(fileLocations.defaultOutputFile("output_setlist.xlsx"));
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
            fileLocations.rememberOutputFile(savedFile);
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
