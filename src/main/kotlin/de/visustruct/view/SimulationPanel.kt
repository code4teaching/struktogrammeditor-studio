package de.visustruct.view

import de.visustruct.control.Controlling
import de.visustruct.control.GlobalSettings
import de.visustruct.control.Struktogramm
import de.visustruct.i18n.I18n
import de.visustruct.simulation.SimulationEngine
import de.visustruct.simulation.SimulationInputRequest
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.UIManager
import javax.swing.border.TitledBorder
import kotlin.collections.ArrayList

/**
 * Simulations-Ansicht: Steuerung, Eingabe, Variablen, aktueller Block, Ausgabe (von oben nach unten).
 * Diagramm-Markierung: [GlobalSettings.getSimulationHighlightMode].
 */
class SimulationPanel(private val controlling: Controlling) : JPanel(BorderLayout(8, 8)) {

    private companion object {
        private const val serialVersionUID = 1L
        private const val SYM_PLAY = "\u25B6"
        private const val SYM_PAUSE = "\u23F8"
        private const val SYM_STEP = "\u2192"
        private const val SYM_STOP = "\u23F9"
        private const val SYM_SUBMIT = "\u2713"
    }

    private var engine: SimulationEngine? = null

    private val variablesArea = JTextArea(4, 40)
    private val currentBlockArea = JTextArea(3, 40)
    private val outputArea = JTextArea(6, 40)
    private val messageLabel = JLabel(" ")
    private val traceLabel = JLabel(" ")
    private val inputPromptLabel = JLabel(" ")
    private val inputField = JTextField(24)
    private val inputSubmitButton = JButton()
    private val playButton = JButton()
    private val pauseButton = JButton()
    private val stepButton = JButton()
    private val stopButton = JButton()

    private val playTimer: Timer
    private var playing = false
    private var resumePlayAfterInput = false

    private lateinit var controlsNorthPanel: JPanel
    private lateinit var inputSectionPanel: JPanel
    private lateinit var currentBlockPanel: JPanel
    private lateinit var variablesScroll: JScrollPane
    private lateinit var outputScroll: JScrollPane

    private var lastExecutedSimulationPath: List<Int>? = null

    init {
        border = BorderFactory.createEmptyBorder(12, 12, 12, 12)

        val mono = Font.decode(Font.MONOSPACED + "-PLAIN-13")
        variablesArea.font = mono
        currentBlockArea.font = mono
        outputArea.font = mono
        variablesArea.isEditable = false
        currentBlockArea.isEditable = false
        outputArea.isEditable = false
        currentBlockArea.lineWrap = true
        currentBlockArea.wrapStyleWord = true

        val statusFont = mono.deriveFont(Font.ITALIC, 12f)
        messageLabel.font = statusFont
        traceLabel.font = statusFont

        playTimer = Timer(GlobalSettings.getSimulationPlayDelayMs()) { onPlayTick() }
        playTimer.isRepeats = true

        playButton.addActionListener { onPlay() }
        pauseButton.addActionListener { onPause() }
        stepButton.addActionListener { onStep() }
        stopButton.addActionListener { onStop() }
        inputSubmitButton.addActionListener { onSubmitInput() }
        inputField.addActionListener { onSubmitInput() }

        val transport = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0))
        transport.add(playButton)
        transport.add(pauseButton)
        transport.add(stepButton)
        transport.add(stopButton)

        controlsNorthPanel = JPanel(BorderLayout(0, 4))
        controlsNorthPanel.border = BorderFactory.createTitledBorder(I18n.tr("simulation.controls"))
        controlsNorthPanel.add(transport, BorderLayout.NORTH)
        controlsNorthPanel.add(messageLabel, BorderLayout.SOUTH)

        inputSectionPanel = JPanel(BorderLayout(0, 6))
        inputSectionPanel.border = BorderFactory.createTitledBorder(I18n.tr("simulation.input"))
        inputSectionPanel.add(inputPromptLabel, BorderLayout.NORTH)
        val inputRow = JPanel(BorderLayout(8, 0))
        inputRow.add(inputField, BorderLayout.CENTER)
        inputRow.add(inputSubmitButton, BorderLayout.EAST)
        inputSectionPanel.add(inputRow, BorderLayout.CENTER)
        inputSectionPanel.preferredSize = Dimension(260, 72)

        variablesScroll = JScrollPane(variablesArea)
        variablesScroll.border = BorderFactory.createTitledBorder(I18n.tr("simulation.variables"))
        variablesScroll.preferredSize = Dimension(260, 88)

        currentBlockPanel = JPanel(BorderLayout(0, 4))
        currentBlockPanel.border = BorderFactory.createTitledBorder(I18n.tr("simulation.path"))
        currentBlockPanel.add(JScrollPane(currentBlockArea), BorderLayout.CENTER)
        currentBlockPanel.add(traceLabel, BorderLayout.SOUTH)
        currentBlockPanel.preferredSize = Dimension(260, 96)

        outputScroll = JScrollPane(outputArea)
        outputScroll.border = BorderFactory.createTitledBorder(I18n.tr("simulation.output"))

        val topStack = JPanel()
        topStack.layout = BoxLayout(topStack, BoxLayout.Y_AXIS)
        topStack.add(controlsNorthPanel)
        topStack.add(Box.createVerticalStrut(8))
        topStack.add(inputSectionPanel)
        topStack.add(Box.createVerticalStrut(8))
        topStack.add(variablesScroll)
        topStack.add(Box.createVerticalStrut(8))
        topStack.add(currentBlockPanel)

        add(topStack, BorderLayout.NORTH)
        add(outputScroll, BorderLayout.CENTER)

        applyTransportLabelsAndTips()
        minimumSize = Dimension(260, 320)
        clearEngine()
    }

    private fun copyPath(path: List<Int>?): List<Int>? {
        if (path == null || path.isEmpty()) return null
        return ArrayList(path)
    }

    private fun isStrictPathExtension(before: List<Int>?, after: List<Int>?): Boolean {
        if (before == null || after == null || after.size <= before.size) return false
        for (i in before.indices) {
            if (before[i] != after[i]) return false
        }
        return true
    }

    private fun recordStepHighlightPath(pathBeforeStep: List<Int>?) {
        if (engine == null) return
        val after = copyPath(engine!!.currentStepPath)
        lastExecutedSimulationPath =
            if (isStrictPathExtension(pathBeforeStep, after)) after else pathBeforeStep
    }

    private fun applyTransportLabelsAndTips() {
        val symFont = buttonSymbolFont()

        playButton.font = symFont
        playButton.text = SYM_PLAY
        playButton.margin = Insets(4, 10, 4, 10)
        playButton.toolTipText = I18n.tr("simulation.play.tooltip")
        playButton.accessibleContext.accessibleName = I18n.tr("simulation.play")

        pauseButton.font = symFont
        pauseButton.text = SYM_PAUSE
        pauseButton.margin = Insets(4, 10, 4, 10)
        pauseButton.toolTipText = I18n.tr("simulation.pause.tooltip")
        pauseButton.accessibleContext.accessibleName = I18n.tr("simulation.pause")

        stepButton.font = symFont
        stepButton.text = SYM_STEP
        stepButton.margin = Insets(4, 10, 4, 10)
        stepButton.toolTipText = I18n.tr("simulation.step.tooltip")
        stepButton.accessibleContext.accessibleName = I18n.tr("simulation.step")

        stopButton.font = symFont
        stopButton.text = SYM_STOP
        stopButton.margin = Insets(4, 10, 4, 10)
        stopButton.toolTipText = I18n.tr("simulation.stop.tooltip")
        stopButton.accessibleContext.accessibleName = I18n.tr("simulation.stop")

        inputSubmitButton.font = symFont
        inputSubmitButton.text = SYM_SUBMIT
        inputSubmitButton.margin = Insets(4, 10, 4, 10)
        inputSubmitButton.toolTipText = I18n.tr("simulation.input.submit.tooltip")
        inputSubmitButton.accessibleContext.accessibleName = I18n.tr("simulation.input.submit")
    }

    private fun buttonSymbolFont(): Font {
        var base: Font? = UIManager.getFont("Button.font")
        if (base == null) base = Font.decode(Font.SANS_SERIF + "-PLAIN-13")
        return base.deriveFont(Font.PLAIN, maxOf(15f, base.size2D + 3f))
    }

    fun onSimulationSettingsChanged() {
        playTimer.delay = GlobalSettings.getSimulationPlayDelayMs()
        if (engine != null) {
            refreshFromEngine()
        }
    }

    private fun clearDiagramSimulationHighlight() {
        val str = controlling.gibAktuellesStruktogramm()
        str?.setzeSimulationSpotlightPfad(null)
    }

    private fun applyDiagramHighlight() {
        val str = controlling.gibAktuellesStruktogramm()
        if (str == null) return
        if (engine == null) {
            str.setzeSimulationSpotlightPfad(null)
            return
        }
        str.setzeSimulationSpotlightPfad(resolveHighlightPath())
    }

    private fun resolveHighlightPath(): List<Int>? {
        if (engine == null) return null
        val current = copyPath(engine!!.currentStepPath)
        val simAmEnde = engine!!.getInputRequestForUi() == null && !engine!!.canStep

        if (GlobalSettings.isSimulationHighlightNextStep()) {
            if (current != null && current.isNotEmpty()) return current
            if (simAmEnde && lastExecutedSimulationPath != null) {
                return copyPath(lastExecutedSimulationPath)
            }
            return null
        }

        val last = copyPath(lastExecutedSimulationPath)
        if (last != null && last.isNotEmpty()) return last
        if (!simAmEnde && current != null && current.isNotEmpty()) return current
        return null
    }

    private fun resolveCurrentBlockDisplayText(): String {
        if (engine == null) return ""
        val highlightPath = resolveHighlightPath()
        val stepText = engine!!.findStepTextByPath(highlightPath)
        if (stepText != null && stepText.isNotBlank()) return stepText.trim()
        if (!engine!!.canStep) {
            val msg = engine!!.getUiMessage()
            if (msg != null && msg.isNotBlank()) return msg.trim()
            return "—"
        }
        return "—"
    }

    private fun updateStatusAndBlockPanels() {
        val msg = engine?.getUiMessage()
        val showStatus =
            msg != null && msg.isNotBlank() &&
                (engine!!.getInputRequestForUi() == null || engine!!.getInputErrorForUi() == null)
        messageLabel.text = if (showStatus) msg!!.trim() else " "
        messageLabel.isVisible = showStatus

        currentBlockArea.text = resolveCurrentBlockDisplayText()

        val tr = engine?.getUiLastTrace()
        val showTrace = tr != null && tr.isNotBlank()
        traceLabel.text = if (showTrace) tr!!.trim() else " "
        traceLabel.isVisible = showTrace
    }

    override fun removeNotify() {
        stopPlayInternal()
        playTimer.stop()
        super.removeNotify()
    }

    fun setEngine(eng: SimulationEngine?) {
        stopPlayInternal()
        resumePlayAfterInput = false
        lastExecutedSimulationPath = null
        engine = eng
        refreshFromEngine()
    }

    fun clearEngine() {
        stopPlayInternal()
        resumePlayAfterInput = false
        engine = null
        lastExecutedSimulationPath = null
        clearDiagramSimulationHighlight()
        variablesArea.text = ""
        currentBlockArea.text = ""
        outputArea.text = ""
        messageLabel.text = " "
        messageLabel.isVisible = false
        traceLabel.text = " "
        traceLabel.isVisible = false
        inputPromptLabel.text = " "
        inputField.text = ""
        setInputSectionIdle()
        updateTransportButtons()
    }

    private fun setInputSectionIdle() {
        inputPromptLabel.text = I18n.tr("simulation.input.idle")
        inputField.text = ""
        inputField.isEnabled = false
        inputSubmitButton.isEnabled = false
    }

    fun refreshFromEngine() {
        if (engine == null) {
            clearEngine()
            return
        }

        val vars = StringBuilder()
        for (e in java.util.TreeMap(engine!!.getVariablesSnapshot())) {
            vars.append(e.key).append(" = ").append(e.value).append('\n')
        }
        variablesArea.text = vars.toString()

        val out = StringBuilder()
        for (line in engine!!.getOutputLinesSnapshot()) {
            out.append(line).append('\n')
        }
        val outText = out.toString()
        outputArea.text = outText
        if (outText.isNotEmpty()) {
            outputArea.caretPosition = outText.length
        }

        updateStatusAndBlockPanels()

        val req: SimulationInputRequest? = engine!!.getInputRequestForUi()
        val err = engine!!.getInputErrorForUi()
        if (req != null) {
            if (playing) {
                resumePlayAfterInput = true
            }
            stopPlayInternal()
            inputPromptLabel.text = req.prompt
            inputField.isEnabled = true
            inputSubmitButton.isEnabled = true
            if (err != null && err.isNotEmpty()) {
                messageLabel.text = err
                messageLabel.isVisible = true
            }
            SwingUtilities.invokeLater { inputField.requestFocusInWindow() }
        } else {
            setInputSectionIdle()
        }

        updateTransportButtons()
        applyDiagramHighlight()
    }

    private fun stopPlayInternal() {
        playing = false
        playTimer.stop()
    }

    private fun updateTransportButtons() {
        val hasEngine = engine != null
        val canStep = hasEngine && engine!!.canStep
        playButton.isEnabled = hasEngine && !playing && canStep
        pauseButton.isEnabled = hasEngine && playing
        stepButton.isEnabled = hasEngine && canStep
        stopButton.isEnabled = hasEngine
    }

    private fun onPlay() {
        if (engine == null || !engine!!.canStep) return
        playTimer.delay = GlobalSettings.getSimulationPlayDelayMs()
        playing = true
        playTimer.start()
        updateTransportButtons()
    }

    private fun onPause() {
        resumePlayAfterInput = false
        stopPlayInternal()
        updateTransportButtons()
    }

    private fun onPlayTick() {
        if (engine == null || !playing) return
        if (!engine!!.canStep) {
            stopPlayInternal()
            refreshFromEngine()
            return
        }
        val before = copyPath(engine!!.currentStepPath)
        engine!!.step()
        recordStepHighlightPath(before)
        refreshFromEngine()
        if (engine != null && engine!!.getInputRequestForUi() != null) {
            stopPlayInternal()
            updateTransportButtons()
        } else if (engine == null || !engine!!.canStep) {
            stopPlayInternal()
            updateTransportButtons()
        }
    }

    private fun onStep() {
        if (engine == null) return
        resumePlayAfterInput = false
        stopPlayInternal()
        if (!engine!!.canStep) {
            updateTransportButtons()
            applyDiagramHighlight()
            return
        }
        val before = copyPath(engine!!.currentStepPath)
        engine!!.step()
        recordStepHighlightPath(before)
        refreshFromEngine()
    }

    private fun onStop() {
        if (engine == null) return
        resumePlayAfterInput = false
        stopPlayInternal()
        lastExecutedSimulationPath = null
        engine!!.reset(null)
        inputField.text = ""
        refreshFromEngine()
    }

    private fun onSubmitInput() {
        if (engine == null || !inputField.isEnabled) return
        val shouldResumePlay = resumePlayAfterInput
        val before = copyPath(engine!!.currentStepPath)
        engine!!.provideInput(inputField.text)
        inputField.text = ""
        recordStepHighlightPath(before)
        refreshFromEngine()
        if (shouldResumePlay && engine != null &&
            engine!!.getInputRequestForUi() == null && engine!!.canStep
        ) {
            resumePlayAfterInput = false
            onPlay()
        }
    }

    fun refreshLocalizedTexts() {
        applyTransportLabelsAndTips()
        setTitledBorderTitle(controlsNorthPanel, I18n.tr("simulation.controls"))
        setTitledBorderTitle(inputSectionPanel, I18n.tr("simulation.input"))
        setTitledBorderTitle(variablesScroll, I18n.tr("simulation.variables"))
        setTitledBorderTitle(currentBlockPanel, I18n.tr("simulation.path"))
        setTitledBorderTitle(outputScroll, I18n.tr("simulation.output"))
        if (engine != null) {
            refreshFromEngine()
        } else {
            clearEngine()
        }
    }

    private fun setTitledBorderTitle(panel: JPanel, title: String) {
        if (panel.border is TitledBorder) {
            (panel.border as TitledBorder).title = title
        }
    }

    private fun setTitledBorderTitle(scroll: JScrollPane, title: String) {
        if (scroll.border is TitledBorder) {
            (scroll.border as TitledBorder).title = title
        }
    }
}
