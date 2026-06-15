// composables/useExport.ts — 多格式导出
import { ref, type Ref } from 'vue'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import ExcelJS from 'exceljs'

export function useExport(companyName: Ref<string>) {
  const exporting = ref(false)
  const format = ref<string | null>(null)

  function filename(fmt: string): string {
    const date = new Date().toISOString().slice(0, 10)
    return `${companyName.value}_财报分析报告_${date}.${fmt}`
  }

  // 获取导出目标元素
  function getTargetEl(): HTMLElement | null {
    const el = document.querySelector('.dashboard-page')
    if (!el) return null
    const clone = el.cloneNode(true) as HTMLElement
    // 将 canvas 替换为 img，解决 html2canvas 无法捕获 canvas 的问题
    replaceCanvasesWithImages(el as HTMLElement, clone)
    return clone
  }

  // 将原 DOM 中的 canvas 转为图片，替换克隆中的对应 canvas
  function replaceCanvasesWithImages(original: HTMLElement, clone: HTMLElement) {
    const origCanvases = original.querySelectorAll('canvas')
    const cloneCanvases = clone.querySelectorAll('canvas')
    if (origCanvases.length !== cloneCanvases.length) return

    for (let i = 0; i < origCanvases.length; i++) {
      const origCanvas = origCanvases[i]
      const cloneCanvas = cloneCanvases[i]
      try {
        const dataUrl = origCanvas.toDataURL('image/png')
        const img = document.createElement('img')
        img.src = dataUrl
        img.width = origCanvas.width
        img.height = origCanvas.height
        img.style.width = cloneCanvas.style.width || '100%'
        img.style.height = cloneCanvas.style.height || 'auto'
        cloneCanvas.parentNode?.replaceChild(img, cloneCanvas)
      } catch (e) {
        // 跨域 canvas 可能无法导出，跳过
        console.warn('Cannot export canvas:', e)
      }
    }
  }

  // 附加免责声明
  function appendDisclaimer(el: HTMLElement) {
    const disc = document.createElement('div')
    disc.className = 'export-disclaimer'
    disc.innerHTML =
      '<p style="color:#92400e;border:1px dashed #f59e0b;padding:12px;margin-top:20px;font-size:12px;border-radius:8px">⚠️ 免责声明：本报告由 SmartReport 自动生成，仅供学习参考，不构成任何投资建议。</p>'
    el.appendChild(disc)
  }

  function triggerDownload(blob: Blob, fmt: string) {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename(fmt)
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  async function exportPNG() {
    exporting.value = true
    format.value = 'png'
    const el = getTargetEl()
    if (!el) { exporting.value = false; return }
    // 临时挂到 DOM（html2canvas 需要可见元素）
    el.style.position = 'absolute'
    el.style.left = '-9999px'
    document.body.appendChild(el)
    appendDisclaimer(el)
    await new Promise(r => setTimeout(r, 200))

    const canvas = await html2canvas(el, { scale: 2, useCORS: true, backgroundColor: '#ffffff' })
    canvas.toBlob(blob => {
      if (blob) triggerDownload(blob, 'png')
    })

    document.body.removeChild(el)
    exporting.value = false
  }

  async function exportPDF() {
    exporting.value = true
    format.value = 'pdf'
    const el = getTargetEl()
    if (!el) { exporting.value = false; return }

    el.style.position = 'absolute'
    el.style.left = '-9999px'
    el.style.width = '800px' // 固定宽度保证截图一致
    document.body.appendChild(el)
    appendDisclaimer(el)
    await new Promise(r => setTimeout(r, 200))

    const canvas = await html2canvas(el, { scale: 2, useCORS: true, backgroundColor: '#ffffff' })
    const imgData = canvas.toDataURL('image/png')

    const pdf = new jsPDF('p', 'mm', 'a4')
    const pageWidth = pdf.internal.pageSize.getWidth()
    const imgHeight = (canvas.height * pageWidth) / canvas.width
    const pageHeight = pdf.internal.pageSize.getHeight()

    let heightLeft = imgHeight
    let position = 0
    pdf.addImage(imgData, 'PNG', 0, position, pageWidth, imgHeight)
    heightLeft -= pageHeight

    while (heightLeft > 0) {
      position = -(imgHeight - heightLeft)
      pdf.addPage()
      pdf.addImage(imgData, 'PNG', 0, position, pageWidth, imgHeight)
      heightLeft -= pageHeight
    }

    pdf.save(filename('pdf'))
    document.body.removeChild(el)
    exporting.value = false
  }

  async function exportWord() {
    exporting.value = true
    format.value = 'doc'
    const el = getTargetEl()
    if (!el) { exporting.value = false; return }

    el.style.position = 'absolute'
    el.style.left = '-9999px'
    document.body.appendChild(el)
    appendDisclaimer(el)
    await new Promise(r => setTimeout(r, 100))

    const html = `<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word">
      <head><meta charset="utf-8"/></head><body>${el.innerHTML}</body></html>`
    const blob = new Blob(['\ufeff' + html], { type: 'application/msword' })
    triggerDownload(blob, 'doc')

    document.body.removeChild(el)
    exporting.value = false
  }

  async function exportExcel(store: any) {
    exporting.value = true
    format.value = 'xlsx'

    const workbook = new ExcelJS.Workbook()
    const sheet = workbook.addWorksheet('财报数据')

    // 从 store 提取数据
    const kpiData = store.kpiData
    const timelineData = store.timelineData

    if (kpiData?.kpis) {
      sheet.addRow(['指标', '数值', '同比变化'])
      for (const kpi of kpiData.kpis) {
        sheet.addRow([kpi.name, `${kpi.value} ${kpi.unit}`, kpi.yoy != null ? `${kpi.yoy}%` : '—'])
      }
    }

    if (timelineData?.metrics) {
      const dataSheet = workbook.addWorksheet('近五年数据')
      const header = ['指标', ...(timelineData.years || [])]
      dataSheet.addRow(header)
      for (const metric of timelineData.metrics) {
        const row = [`${metric.name} (${metric.unit})`]
        for (const v of metric.values) {
          row.push(v != null ? String(v) : '—')
        }
        dataSheet.addRow(row)
      }
    }

    const buf = await workbook.xlsx.writeBuffer()
    const blob = new Blob([buf], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    triggerDownload(blob, 'xlsx')

    exporting.value = false
  }

  return { exporting, format, exportPNG, exportPDF, exportWord, exportExcel }
}
