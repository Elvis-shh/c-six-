export function cleanIndicatorName(name: string): string {
  return name.replace(/\([A-Za-z]+\)/g, '').trim()
}
