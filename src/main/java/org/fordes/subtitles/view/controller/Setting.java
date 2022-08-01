package org.fordes.subtitles.view.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.swing.DesktopUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;
import org.fordes.subtitles.view.enums.ServiceProvider;
import org.fordes.subtitles.view.enums.ServiceType;
import org.fordes.subtitles.view.event.ThemeChangeEvent;
import org.fordes.subtitles.view.event.ToastConfirmEvent;
import org.fordes.subtitles.view.mapper.ConfigMapper;
import org.fordes.subtitles.view.model.ApplicationInfo;
import org.fordes.subtitles.view.model.PO.ServiceInterface;
import org.fordes.subtitles.view.model.PO.Version;
import org.fordes.subtitles.view.service.InterfaceService;
import org.fordes.subtitles.view.utils.FileUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 语音转换 控制器
 *
 * @author fordes on 2022/4/8
 */
@Slf4j
@Component
public class Setting extends DelayInitController {

    @FXML
    private VBox infoPanel;

    @FXML
    private TextFlow tips;

    @FXML
    private ToggleGroup themeGroup, editorModeGroup, exitModeGroup, languageListGroup;

    @FXML
    private JFXComboBox<Version> version;

    @FXML
    private JFXComboBox<ServiceType> type;

    @FXML
    private JFXComboBox<ServiceProvider> provider;

    @FXML
    private JFXComboBox<String> fontFace;

    @FXML
    private JFXComboBox<Integer> fontSize;

    @FXML
    private TextField outPath;

    @Resource
    private InterfaceService interfaceService;

    @Resource
    private ConfigMapper configMapper;

    @Resource
    private ThreadPoolExecutor globalExecutor;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        globalExecutor.execute(() -> {

            //初始化首选项
            fontFace.getItems().addAll(Font.getFontNames());
            fontSize.getItems().addAll(CollUtil.newArrayList(10, 12, 14, 16, 18, 20, 24, 36));
            initPreference();

            //首选项监听事件
            themeGroup.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
                Boolean value = Convert.toBool(t1.getUserData());
                ApplicationInfo.stage.fireEvent(new ThemeChangeEvent(value));
                ApplicationInfo.config.setTheme(value);
            });

            editorModeGroup.selectedToggleProperty().addListener((observableValue, toggle, t1)
                    -> ApplicationInfo.config.setEditMode(Convert.toBool(t1.getUserData())));
            exitModeGroup.selectedToggleProperty().addListener((observableValue, toggle, t1)
                    -> ApplicationInfo.config.setExitMode(Convert.toBool(t1.getUserData())));
            fontFace.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1)
                    -> ApplicationInfo.config.setFontFace(t1));
            fontSize.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1)
                    -> ApplicationInfo.config.setFontSize(t1));
            outPath.textProperty().addListener((observableValue, s, t1)
                    -> ApplicationInfo.config.setOutPath(StrUtil.trim(t1)));
            languageListGroup.selectedToggleProperty().addListener((observableValue, toggle, t1) ->
                    ApplicationInfo.config.setLanguageListMode(Convert.toBool(t1.getUserData())));
            //初始化接口选项
            initInterface();

        });
        super.initialize(url, resourceBundle);
    }

    @Override
    public void delayInit() {
        //监听器用于保存配置
        root.visibleProperty().addListener((observableValue, aBoolean, t1) -> {
            if (!t1) {
                if (FileUtil.exist(ApplicationInfo.config.getOutPath())) {
                    ApplicationInfo.config.setOutPath(outPath.getText().trim());
                } else {
                    outPath.setText(ApplicationInfo.config.getOutPath());
                }
                configMapper.updateById(ApplicationInfo.config);
            } else {
                //每次显示前重新初始化一次
                initPreference();
            }
        });


        super.delayInit();
    }



    void initPreference() {
        //读取配置设置默认值
        fontFace.getSelectionModel().select(ApplicationInfo.config.getFontFace());
        fontSize.getSelectionModel().select(ApplicationInfo.config.getFontSize());
        editorModeGroup.getToggles().forEach(item -> {
            if (Convert.toBool(item.getUserData()).equals(ApplicationInfo.config.getEditMode())) {
                item.setSelected(true);
            }
        });
        themeGroup.getToggles().forEach(item -> {
            if (ObjectUtil.equal(ApplicationInfo.config.getTheme(), Convert.toBool(item.getUserData()))) {
                item.setSelected(true);
            }
        });
        exitModeGroup.getToggles().forEach(item -> {
            if (Convert.toBool(item.getUserData()).equals(ApplicationInfo.config.getExitMode())) {
                item.setSelected(true);
            }
        });
        outPath.setText(ApplicationInfo.config.getOutPath());

    }

    void initInterface() {
        //接口类型
        type.getItems().addAll(ServiceType.values());
        //服务商
        provider.getItems().addAll(ServiceProvider.values());


        type.getSelectionModel().selectedItemProperty().addListener((observableValue, type, t1) -> {
            if (null != t1 && provider.getValue() != null) {
                version.getItems().clear();
                version.getItems().addAll(interfaceService.getVersions(t1, provider.getValue()));
            }
        });

        provider.getSelectionModel().selectedItemProperty().addListener((observableValue, supportDto, t1) -> {
            if (null != t1 && type.getValue() != null) {
                version.getItems().clear();
                version.getItems().addAll(interfaceService.getVersions(type.getValue(), t1));
            }
        });

        version.getSelectionModel().selectedItemProperty().addListener((observableValue, serviceVersion, t1) -> {
            if (null != t1) {
                tips.setVisible(false);
                version.setTooltip(new Tooltip(t1.getRemark()));
                buildInfoFrame(interfaceService.getInterface(type.getValue(), provider.getValue()));
            } else {
                tips.setVisible(true);
            }
        });
        //提示区
        tips.visibleProperty().addListener((observableValue, aBoolean, t1) -> infoPanel.setVisible(!t1));
    }

    void buildInfoFrame(ServiceInterface info) {
        infoPanel.getChildren().clear();

        (info.getAuth() == null ? info.getTemplate(): info.getAuth()).forEach((k, v) -> {

            HBox hBox = new HBox();
            hBox.setMinHeight(90);
            hBox.setAlignment(Pos.CENTER_LEFT);

            Label label = new Label(k);
            label.setMinSize(120, 90);
            label.getStyleClass().add("item");
            HBox.setMargin(label, new Insets(0,0,0,30));
            hBox.getChildren().add(label);

            TextField textField = new TextField(ObjectUtil.isNotEmpty(v) ? v.toString() : StrUtil.EMPTY);
            textField.getStyleClass().add("item");
            textField.setUserData(k);
            textField.setMinSize(140, 90);
            hBox.getChildren().add(textField);
            infoPanel.getChildren().add(hBox);
        });

        JFXButton save = new JFXButton("保存");
        save.setPrefSize(80, 30);
        save.getStyleClass().add("normal-button");
        save.setUserData(info);
        save.setOnAction(event -> {

            JSONObject param = new JSONObject();
            infoPanel.getChildren().forEach(e -> {
                if (e instanceof TextField) {
                    param.putOpt((String) e.getUserData(), ((TextField) e).getText());
                }
            });
            ServiceInterface data = (ServiceInterface) save.getUserData();
            data.setVersion(version.getValue().getId()).setAuth(param.toString());
            try {
                if (interfaceService.updateById(info)) {
                    tips.setVisible(true);
                    ApplicationInfo.stage.fireEvent(new ToastConfirmEvent("保存成功", "接口信息已经保存"));
                    return;
                }
            } catch (Exception e) {
                log.error("接口信息保存失败 => {}", JSONUtil.toJsonStr(info));
                log.error(ExceptionUtil.stacktraceToString(e));
            }
            ApplicationInfo.stage.fireEvent(new ToastConfirmEvent("保存失败", "数据操作失败，错误已记录"));
        });
        HBox hBox = new HBox();
        hBox.setMinHeight(90);
        hBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setMargin(save, new Insets(0,30,0,0));
        hBox.getChildren().add(save);

        if (StrUtil.isNotEmpty(info.getPage())) {
            JFXButton applyFor = new JFXButton("去申请");
            applyFor.setPrefSize(80, 30);
            applyFor.getStyleClass().add("normal-button");
            applyFor.setTooltip(new Tooltip(info.getPage()));
            applyFor.setOnAction(event -> DesktopUtil.browse(info.getPage()));
            hBox.getChildren().add(applyFor);
        }
        infoPanel.getChildren().add(hBox);
    }

    @FXML
    private void onChooseOutPath(MouseEvent event) {
        File path = FileUtils.choosePath(outPath.getText().trim()).showDialog(ApplicationInfo.stage);
        if (path != null && StrUtil.isNotEmpty(path.getPath())) {
            outPath.setText(path.getPath());
        }
        event.consume();
    }
}
